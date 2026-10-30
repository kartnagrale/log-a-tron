#!/usr/bin/env bash
# Local function tests; curl and collector operations are replaced with fixtures.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"
export LOGATRON_PLATFORM_URL=https://control.test
export LOGATRON_AGENT_ID=00000000-0000-0000-0000-000000000001
export LOGATRON_AGENT_TOKEN=test-token
export LOGATRON_STATE_DIR="$WORK/state"
export LOGATRON_SHIPPER_TEST_LIBRARY=true
# Git Bash lacks flock; this fixture exercises deployment operations, not OS locking.
flock(){ return 0; }
source "$ROOT/logatron-managed-shipper.sh"
trap 'rm -rf "$WORK"' EXIT
heartbeat(){ printf '%s|%s\n' "$1" "${2:-}" > "$WORK/heartbeat"; }
log(){ :; }
OTELCOL_BIN="$WORK/collector"
printf '#!/usr/bin/env bash\nexit 0\n' > "$OTELCOL_BIN"
chmod +x "$OTELCOL_BIN"
start_collector(){ printf 'restart\n' >> "$WORK/restarts"; return 0; }
DEPLOYMENT_ID=00000000-0000-0000-0000-000000000002
hash(){ python3 -c 'import hashlib,sys;print(hashlib.sha256(open(sys.argv[1],"rb").read()).hexdigest())' "$1"; }

printf 'old configuration\n' > "$ACTIVE_CONFIG"
old="$(hash "$ACTIVE_CONFIG")"
printf '%s\n' "$old" > "$HASH_FILE"; printf '1\n' > "$VERSION_FILE"
printf 'new configuration\n' > "$TMP_CONFIG"; desired="$(hash "$TMP_CONFIG")"
apply_config_if_changed "$desired" 2
[[ "$(cat "$HASH_FILE")" == "$desired" && "$(cat "$VERSION_FILE")" == 2 ]]
[[ "$(cat "$PREVIOUS_CONFIG")" == 'old configuration' ]]
[[ "$(wc -l < "$WORK/restarts")" == 1 ]]

# Re-polling the identical immutable version never restarts.
apply_config_if_changed "$desired" 2
[[ "$(wc -l < "$WORK/restarts")" == 1 ]]

# A failed startup restores both configuration and acknowledged identity.
printf 'bad configuration\n' > "$TMP_CONFIG"; bad="$(hash "$TMP_CONFIG")"
attempt=0
start_collector(){ attempt=$((attempt+1)); ((attempt>1)); }
apply_config_if_changed "$bad" 3 && exit 1
[[ "$(cat "$HASH_FILE")" == "$desired" && "$(cat "$VERSION_FILE")" == 2 ]]
[[ "$(cat "$ACTIVE_CONFIG")" == 'new configuration' ]]
[[ "$(cat "$WORK/heartbeat")" == DEGRADED* ]]

# Target validation failure must not replace active files or restart.
printf '#!/usr/bin/env bash\nexit 1\n' > "$OTELCOL_BIN"
printf 'invalid yaml\n' > "$TMP_CONFIG"; invalid="$(hash "$TMP_CONFIG")"
attempt=0
apply_config_if_changed "$invalid" 4 && exit 1
[[ "$attempt" == 0 && "$(cat "$ACTIVE_CONFIG")" == 'new configuration' ]]

# A new version without an explicit rollout identifier is refused.
DEPLOYMENT_ID=''
printf 'unauthorized\n' > "$TMP_CONFIG"
apply_config_if_changed "$(hash "$TMP_CONFIG")" 5 && exit 1
[[ "$attempt" == 0 && "$(cat "$VERSION_FILE")" == 2 ]]
printf 'managed shipper: activation, idempotency, validation failure, rollback and authorization passed\n'
