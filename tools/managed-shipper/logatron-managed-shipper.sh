#!/usr/bin/env bash
set -euo pipefail
umask 077
# managed-v2: explicit configuration releases only.

: "${LOGATRON_PLATFORM_URL:?Set the HTTPS control-plane URL}"
: "${LOGATRON_AGENT_ID:?Set LOGATRON_AGENT_ID from Administration}"
: "${LOGATRON_AGENT_TOKEN:?Set LOGATRON_AGENT_TOKEN from the one-time registration secret}"

OTELCOL_BIN="${OTELCOL_BIN:-/opt/logatron/otelcol-contrib}"
STATE_DIR="${LOGATRON_STATE_DIR:-/var/lib/logatron-agent}"
POLL_SECONDS="${LOGATRON_POLL_SECONDS:-15}"
ACTIVE_CONFIG="$STATE_DIR/collector.yaml"
PREVIOUS_CONFIG="$STATE_DIR/collector.yaml.previous"
TMP_CONFIG="$STATE_DIR/collector.yaml.next"
HEADER_FILE="$STATE_DIR/config.headers"
HASH_FILE="$STATE_DIR/applied.sha256"
VERSION_FILE="$STATE_DIR/applied.version"
SHIPPER_LOG="$STATE_DIR/shipper.log"
COLLECTOR_LOG="$STATE_DIR/collector.log"
TOKEN_HEADER="X-Logatron-Agent-Token: $LOGATRON_AGENT_TOKEN"
COLLECTOR_PID=""
DEPLOYMENT_ID=""
LAST_ERROR=""
HEALTHY=false

if [[ "$LOGATRON_PLATFORM_URL" != https://* && "${LOGATRON_ALLOW_HTTP:-false}" != true ]]; then echo "HTTPS required; explicitly authorize HTTP only for development" >&2; exit 2; fi
[[ "$POLL_SECONDS" =~ ^[0-9]+$ ]] && ((POLL_SECONDS>=5 && POLL_SECONDS<=300)) || exit 2
[[ "$LOGATRON_AGENT_ID" =~ ^[A-Fa-f0-9-]{36}$ ]] || exit 2
mkdir -p "$STATE_DIR/storage"
chmod 700 "$STATE_DIR"
exec 9>"$STATE_DIR/shipper.lock"
flock -n 9 || { echo "Another shipper owns this state directory" >&2; exit 2; }
AUTH_FILE="$STATE_DIR/curl-auth"
[[ "$LOGATRON_AGENT_TOKEN" =~ ^[A-Za-z0-9_-]+$ ]] || exit 2
printf 'header = "X-Logatron-Agent-Token: %s"\n' "$LOGATRON_AGENT_TOKEN" > "$AUTH_FILE"
unset LOGATRON_AGENT_TOKEN TOKEN_HEADER
touch "$SHIPPER_LOG" "$COLLECTOR_LOG"

log(){ printf '%s %s\n' "$(date -Is)" "$*" | tee -a "$SHIPPER_LOG"; }
json_escape(){ python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'; }

collector_version(){ "$OTELCOL_BIN" --version 2>/dev/null | head -n1 | tr -d '\r' || true; }

stop_collector(){
  if [[ -n "${COLLECTOR_PID:-}" ]] && kill -0 "$COLLECTOR_PID" 2>/dev/null; then
    kill "$COLLECTOR_PID" 2>/dev/null || true
    for _ in {1..20}; do kill -0 "$COLLECTOR_PID" 2>/dev/null || break; sleep .25; done
    kill -9 "$COLLECTOR_PID" 2>/dev/null || true
    wait "$COLLECTOR_PID" 2>/dev/null || true
  fi
  COLLECTOR_PID=""
  HEALTHY=false
}

start_collector(){
  stop_collector
  log "starting OpenTelemetry Collector with $ACTIVE_CONFIG"
  "$OTELCOL_BIN" --config="$ACTIVE_CONFIG" >>"$COLLECTOR_LOG" 2>&1 9>&- &
  COLLECTOR_PID=$!
  HEALTHY=false
  for _ in {1..10}; do
    if kill -0 "$COLLECTOR_PID" 2>/dev/null && curl --noproxy '*' -fsS --max-time 2 http://127.0.0.1:13133/ >/dev/null; then HEALTHY=true; return 0; fi
    sleep 1
  done
  return 1
}

heartbeat(){
  local status="$1" error="${2:-}" applied="" applied_version=""
  HEALTHY=false
  if [[ -n "$COLLECTOR_PID" ]] && kill -0 "$COLLECTOR_PID" 2>/dev/null && curl --noproxy '*' -fsS --max-time 2 http://127.0.0.1:13133/ >/dev/null; then HEALTHY=true; elif [[ "$status" == ONLINE ]]; then status=DEGRADED; error="Collector health check unavailable"; fi
  [[ -n "$LAST_ERROR" && -z "$error" ]] && error="$LAST_ERROR"
  [[ -n "$error" ]] && status=DEGRADED
  [[ -f "$VERSION_FILE" ]] && applied_version="$(tr -d '\r\n' < "$VERSION_FILE")"
  [[ -f "$HASH_FILE" ]] && applied="$(tr -d '\r\n' < "$HASH_FILE")"
  local version; version="$(collector_version)"
  local body
  body=$(printf '{"collectorVersion":%s,"appliedConfigHash":%s,"appliedConfigVersion":%s,"status":"%s","lastError":%s,"protocol":"managed-v2","deploymentId":%s,"healthVerified":%s}' \
    "$(printf '%s' "$version" | json_escape)" "$(printf '%s' "$applied" | json_escape)" "${applied_version:-null}" "$status" "$(printf '%s' "$error" | json_escape)" "$(if [[ -n "$DEPLOYMENT_ID" ]]; then printf '%s' "$DEPLOYMENT_ID" | json_escape; else printf null; fi)" "$HEALTHY")
  curl -fsS --max-time 10 -X POST --config "$AUTH_FILE" -H 'Content-Type: application/json' --data "$body" \
    "$LOGATRON_PLATFORM_URL/api/internal/v1/agents/$LOGATRON_AGENT_ID/heartbeat" >/dev/null || true
}

fetch_config(){
  rm -f "$TMP_CONFIG" "$HEADER_FILE"
  local code
  code=$(curl -sS --connect-timeout 5 --max-time 15 -w '%{http_code}' -D "$HEADER_FILE" -o "$TMP_CONFIG" --config "$AUTH_FILE" \
    "$LOGATRON_PLATFORM_URL/api/internal/v1/agents/$LOGATRON_AGENT_ID/config") || return 1
  [[ "$code" == 204 ]] && return 0
  [[ "$code" == 200 ]] || return 1
  awk 'tolower($1)=="x-logatron-config-hash:" {gsub("\r",""); hash=$2} tolower($1)=="x-logatron-config-version:" {gsub("\r",""); version=$2} END {if(hash!=""&&version!="") print hash "|" version}' "$HEADER_FILE"
}

rollback(){
  if [[ -f "$PREVIOUS_CONFIG" ]]; then
    log "rolling back to previous collector configuration"
    cp "$PREVIOUS_CONFIG" "$TMP_CONFIG"; mv -f "$TMP_CONFIG" "$ACTIVE_CONFIG"
    [[ -f "$HASH_FILE.previous" ]] && cp "$HASH_FILE.previous" "$HASH_FILE"
    [[ -f "$VERSION_FILE.previous" ]] && cp "$VERSION_FILE.previous" "$VERSION_FILE"
    start_collector || LAST_ERROR="Rollback collector failed its health check; operator intervention required"
  else
    rm -f "$ACTIVE_CONFIG" "$HASH_FILE" "$VERSION_FILE"
    stop_collector
  fi
}

apply_config_if_changed(){
  local desired="$1" desired_version="$2" applied="" applied_version="" computed=""
  [[ -f "$VERSION_FILE" ]] && applied_version="$(tr -d '\r\n' < "$VERSION_FILE")"
  [[ -f "$HASH_FILE" ]] && applied="$(tr -d '\r\n' < "$HASH_FILE")"
  if [[ "$desired" == "$applied" ]] && [[ "$desired_version" == "$applied_version" ]] && [[ -f "$ACTIVE_CONFIG" ]]; then return 0; fi

  [[ "$DEPLOYMENT_ID" =~ ^[A-Fa-f0-9-]{36}$ ]] || { LAST_ERROR="New configuration has no authorized rollout"; return 1; }
  computed="$(python3 -c 'import hashlib,sys; print(hashlib.sha256(open(sys.argv[1],"rb").read()).hexdigest())' "$TMP_CONFIG")"
  if [[ "$computed" != "$desired" ]]; then
    log "config integrity check failed: expected $desired but received $computed"
    LAST_ERROR="Configuration hash mismatch"
    heartbeat DEGRADED "$LAST_ERROR"
    rm -f "$TMP_CONFIG"
    return 1
  fi

  log "validating desired collector config $desired"
  if ! validation=$(timeout 20 "$OTELCOL_BIN" validate --config="$TMP_CONFIG" 2>&1); then
    LAST_ERROR="Target Collector rejected configuration; active configuration retained"
    log "$LAST_ERROR"
    heartbeat DEGRADED "$LAST_ERROR"
    rm -f "$TMP_CONFIG"
    return 1
  fi

  rm -f "$PREVIOUS_CONFIG"
  [[ -f "$ACTIVE_CONFIG" ]] && cp -f "$ACTIVE_CONFIG" "$PREVIOUS_CONFIG"
  [[ -f "$HASH_FILE" ]] && cp "$HASH_FILE" "$HASH_FILE.previous"
  [[ -f "$VERSION_FILE" ]] && cp "$VERSION_FILE" "$VERSION_FILE.previous"
  mv -f "$TMP_CONFIG" "$ACTIVE_CONFIG"
  if start_collector; then
    printf '%s\n' "$desired" > "$HASH_FILE"
    printf '%s\n' "$desired_version" > "$VERSION_FILE"
    LAST_ERROR=""
    log "applied collector config $desired"
    heartbeat ONLINE ""
    return 0
  fi

  local failure="collector failed after applying config $desired; previous config restored"
  LAST_ERROR="$failure"
  rollback
  heartbeat DEGRADED "$failure"
  return 1
}

cleanup(){ log "managed shipper stopping"; stop_collector; rm -f "$AUTH_FILE" "$TMP_CONFIG" "$HEADER_FILE"; }
trap cleanup EXIT
trap 'exit 0' INT TERM

# Tests may load the fixed operations without running the polling loop.
if [[ "${LOGATRON_SHIPPER_TEST_LIBRARY:-false}" == true ]]; then return 0 2>/dev/null || exit 0; fi

if [[ ! -x "$OTELCOL_BIN" ]]; then echo "OTELCOL_BIN is not executable: $OTELCOL_BIN" >&2; exit 2; fi
command -v curl >/dev/null || { echo "curl is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 2; }

command -v flock >/dev/null || exit 2
command -v timeout >/dev/null || exit 2
version="$(collector_version)"
[[ "$version" == *otelcol-contrib* && "$version" =~ (^|[[:space:]])0\.135\.0($|[[:space:]]) ]] || { echo "Requires otelcol-contrib 0.135.0" >&2; exit 2; }
heartbeat DEGRADED ""
log "LOG-A-TRON managed shipper started for agent $LOGATRON_AGENT_ID"

while true; do
  if desired_response=$(fetch_config 2>>"$SHIPPER_LOG"); then
    if [[ -n "$desired_response" ]]; then
      DEPLOYMENT_ID="$(awk 'tolower($1)=="x-logatron-deployment-id:" {gsub("\r",""); print $2}' "$HEADER_FILE")"
      desired="${desired_response%%|*}"
      desired_version="${desired_response#*|}"
      apply_config_if_changed "$desired" "$desired_version" || true
    fi
  else
    log "unable to fetch desired configuration"
    heartbeat DEGRADED "unable to reach LOG-A-TRON control plane"
  fi

  if [[ -f "$ACTIVE_CONFIG" ]]; then
    if [[ -z "${COLLECTOR_PID:-}" ]] || ! kill -0 "$COLLECTOR_PID" 2>/dev/null; then
      log "collector is not running; attempting restart"
      if start_collector; then heartbeat ONLINE ""; else heartbeat DEGRADED "collector restart failed"; fi
    else heartbeat ONLINE ""; fi
  fi
  sleep "$POLL_SECONDS"
done
