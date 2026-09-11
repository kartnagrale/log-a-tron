#!/usr/bin/env bash
set -euo pipefail

: "${LOGATRON_PLATFORM_URL:?Set LOGATRON_PLATFORM_URL, e.g. http://172.22.22.51:8080}"
: "${LOGATRON_AGENT_ID:?Set LOGATRON_AGENT_ID from Administration}"
: "${LOGATRON_AGENT_TOKEN:?Set LOGATRON_AGENT_TOKEN from the one-time registration secret}"

OTELCOL_BIN="${OTELCOL_BIN:-./otelcol-contrib}"
STATE_DIR="${LOGATRON_STATE_DIR:-./logatron-agent-state}"
POLL_SECONDS="${LOGATRON_POLL_SECONDS:-15}"
ACTIVE_CONFIG="$STATE_DIR/collector.yaml"
TMP_CONFIG="$STATE_DIR/collector.yaml.next"
HEADER_FILE="$STATE_DIR/config.headers"
HASH_FILE="$STATE_DIR/applied.sha256"
SHIPPER_LOG="$STATE_DIR/shipper.log"
COLLECTOR_LOG="$STATE_DIR/collector.log"
TOKEN_HEADER="X-Logatron-Agent-Token: $LOGATRON_AGENT_TOKEN"
COLLECTOR_PID=""

mkdir -p "$STATE_DIR/storage"
touch "$SHIPPER_LOG" "$COLLECTOR_LOG"

log(){ printf '%s %s\n' "$(date -Is)" "$*" | tee -a "$SHIPPER_LOG"; }
json_escape(){ python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'; }

collector_version(){
  "$OTELCOL_BIN" --version 2>/dev/null | head -n1 | tr -d '\r' || true
}

stop_collector(){
  if [[ -n "${COLLECTOR_PID:-}" ]] && kill -0 "$COLLECTOR_PID" 2>/dev/null; then
    kill "$COLLECTOR_PID" 2>/dev/null || true
    for _ in {1..20}; do kill -0 "$COLLECTOR_PID" 2>/dev/null || break; sleep .25; done
    kill -9 "$COLLECTOR_PID" 2>/dev/null || true
  fi
  COLLECTOR_PID=""
}

start_collector(){
  stop_collector
  log "starting OpenTelemetry Collector with $ACTIVE_CONFIG"
  "$OTELCOL_BIN" --config="$ACTIVE_CONFIG" >>"$COLLECTOR_LOG" 2>&1 &
  COLLECTOR_PID=$!
  sleep 1
  if ! kill -0 "$COLLECTOR_PID" 2>/dev/null; then
    log "collector exited during startup"
    return 1
  fi
}

heartbeat(){
  local status="$1" error="${2:-}" applied=""
  [[ -f "$HASH_FILE" ]] && applied="$(tr -d '\r\n' < "$HASH_FILE")"
  local version; version="$(collector_version)"
  local body
  body=$(printf '{"collectorVersion":%s,"appliedConfigHash":%s,"status":"%s","lastError":%s}' \
    "$(printf '%s' "$version" | json_escape)" \
    "$(printf '%s' "$applied" | json_escape)" \
    "$status" \
    "$(printf '%s' "$error" | json_escape)")
  curl -fsS --max-time 10 -X POST \
    -H "$TOKEN_HEADER" -H 'Content-Type: application/json' \
    --data "$body" \
    "$LOGATRON_PLATFORM_URL/api/internal/v1/agents/$LOGATRON_AGENT_ID/heartbeat" >/dev/null || true
}

fetch_config(){
  rm -f "$TMP_CONFIG" "$HEADER_FILE"
  curl -fsS --max-time 15 -D "$HEADER_FILE" -o "$TMP_CONFIG" \
    -H "$TOKEN_HEADER" \
    "$LOGATRON_PLATFORM_URL/api/internal/v1/agents/$LOGATRON_AGENT_ID/config"
  awk 'BEGIN{IGNORECASE=1} /^X-Logatron-Config-Hash:/ {gsub("\r",""); sub(/^[^:]+:[[:space:]]*/,""); print; exit}' "$HEADER_FILE"
}

apply_config_if_changed(){
  local desired="$1" applied=""
  [[ -f "$HASH_FILE" ]] && applied="$(tr -d '\r\n' < "$HASH_FILE")"
  if [[ "$desired" == "$applied" ]] && [[ -f "$ACTIVE_CONFIG" ]]; then
    return 0
  fi
  log "validating desired collector config $desired"
  if ! validation=$($OTELCOL_BIN validate --config="$TMP_CONFIG" 2>&1); then
    log "config validation failed: $validation"
    heartbeat DEGRADED "config validation failed: ${validation:0:2000}"
    rm -f "$TMP_CONFIG"
    return 1
  fi
  mv -f "$TMP_CONFIG" "$ACTIVE_CONFIG"
  if start_collector; then
    printf '%s\n' "$desired" > "$HASH_FILE"
    log "applied collector config $desired"
    heartbeat ONLINE ""
    return 0
  fi
  heartbeat DEGRADED "collector failed after applying config $desired"
  return 1
}

cleanup(){
  log "managed shipper stopping"
  stop_collector
}
trap cleanup EXIT INT TERM

if [[ ! -x "$OTELCOL_BIN" ]]; then
  echo "OTELCOL_BIN is not executable: $OTELCOL_BIN" >&2
  exit 2
fi
command -v curl >/dev/null || { echo "curl is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 2; }

log "LOG-A-TRON managed shipper started for agent $LOGATRON_AGENT_ID"

while true; do
  if desired=$(fetch_config 2>>"$SHIPPER_LOG"); then
    if [[ -n "$desired" ]]; then
      apply_config_if_changed "$desired" || true
    else
      log "config response did not include X-Logatron-Config-Hash"
      heartbeat DEGRADED "configuration response missing desired hash"
    fi
  else
    log "unable to fetch desired configuration"
    heartbeat DEGRADED "unable to reach LOG-A-TRON control plane"
  fi

  if [[ -f "$ACTIVE_CONFIG" ]]; then
    if [[ -z "${COLLECTOR_PID:-}" ]] || ! kill -0 "$COLLECTOR_PID" 2>/dev/null; then
      log "collector is not running; attempting restart"
      if start_collector; then heartbeat ONLINE ""; else heartbeat DEGRADED "collector restart failed"; fi
    else
      heartbeat ONLINE ""
    fi
  fi
  sleep "$POLL_SECONDS"
done
