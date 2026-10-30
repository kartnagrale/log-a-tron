# Managed shipper installation

> This script now implements managed-v2 with explicit reviewed releases. Read [Shipper management](../../docs/SHIPPER-MANAGEMENT.md) for backend destination policy, validation, deployment authorization, state-directory alignment and legacy cutover before using the installation examples below. New source edits no longer deploy automatically. Requires otelcol-contrib 0.135.0, HTTPS (or explicitly authorized development HTTP), flock and timeout.

# LOG-A-TRON Managed Shipper

The managed shipper is installed once on each application server. After installation, project/service/log-source onboarding is driven from the LOG-A-TRON Administration UI rather than by hand-editing collector YAML on the server.

## What the server operator installs once

- an `otelcol-contrib` binary compatible with LOG-A-TRON's generated configuration
- `logatron-managed-shipper.sh`
- `curl` and `python3`
- the one-time agent registration values shown by Administration

The shipper polls only explicitly released immutable versions, validates with the target binary, applies atomically, checks process and health endpoint, and reports the matching rollout identity. A failed startup restores the previous configuration and acknowledged version.

## Registration workflow

1. In LOG-A-TRON Administration create/select the Project, Environment, Server, Service, and Service Instance.
2. Register a managed shipper for the selected server.
3. Select a backend-approved OTLP gateway destination reachable from that server.
4. Copy the returned Agent ID and one-time token. The platform stores only a SHA-256 token hash.
5. Install/start the shipper using the values below.
6. Create/update Log Sources, then open Shippers to generate, validate and review a snapshot. Explicitly authorize and confirm each deployment; source edits never release automatically.

## Interactive start

```bash
chmod +x ./logatron-managed-shipper.sh ./otelcol-contrib

export LOGATRON_PLATFORM_URL='https://CONTROL-PLANE'
export LOGATRON_AGENT_ID='00000000-0000-0000-0000-000000000000'
export LOGATRON_AGENT_TOKEN='ONE_TIME_TOKEN_FROM_UI'
export OTELCOL_BIN='/opt/logatron/otelcol-contrib'
export LOGATRON_STATE_DIR='/var/lib/logatron-agent'
export LOGATRON_POLL_SECONDS='15'

./logatron-managed-shipper.sh
```

The control-plane URL and gateway endpoint are intentionally separate. The shipper uses the control-plane URL to fetch desired state; the generated Collector configuration exports telemetry to the gateway endpoint registered for that server.

## systemd example

Create `/etc/logatron-agent.env` readable only by root:

```text
LOGATRON_PLATFORM_URL=https://CONTROL-PLANE
LOGATRON_AGENT_ID=00000000-0000-0000-0000-000000000000
LOGATRON_AGENT_TOKEN=ONE_TIME_TOKEN_FROM_UI
OTELCOL_BIN=/opt/logatron/otelcol-contrib
LOGATRON_STATE_DIR=/var/lib/logatron-agent
LOGATRON_POLL_SECONDS=15
```

Create `/etc/systemd/system/logatron-managed-shipper.service`:

```ini
[Unit]
Description=LOG-A-TRON Managed Shipper
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=logatron
Group=logatron
EnvironmentFile=/etc/logatron-agent.env
WorkingDirectory=/opt/logatron
ExecStart=/opt/logatron/logatron-managed-shipper.sh
Restart=always
RestartSec=5
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now logatron-managed-shipper
sudo systemctl status logatron-managed-shipper
```

## Desired vs actual state

The Administration UI can display:

- **Desired config hash**: generated from the current enabled log sources for the server
- **Applied config hash**: last hash reported by the shipper
- **In sync**: hashes match
- **Last seen / collector version / last error**

Changing a source does not require SSH. Generate and validate the next snapshot in Shippers, review its diff and explicitly confirm a deployment. Only that released version is picked up by the agent.

## Security notes

The current implementation is intended as the first managed-control-plane milestone. Use HTTPS/mTLS or a protected internal network before production rollout. Treat the agent token as a secret and rotate it if exposed. The token-authenticated endpoints deliberately return non-enumerating failures for invalid agent credentials.
