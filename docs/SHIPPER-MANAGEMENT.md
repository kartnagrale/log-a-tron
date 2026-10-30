# Frontend-driven shipper management

## Architecture and rollout prerequisites

Use **Administration** for catalog onboarding and **Shippers** for generation, review, validation, download and explicit rollout. Both routes use the existing Angular/PrimeNG theme and RBAC. Project administrators administer only their project; global administrators create projects. Every configuration resolves persisted source → instance → service/server → environment → project relationships. The browser supplies resource selectors, not trusted envelope IDs.

Flyway V14 is additive. It adds JSON receiver options, optimistic versions, configuration validation metadata, a selected version, disabled-by-default deployment authorization and deployment history. It does not insert example resource UUIDs, alter historical IDs, delete catalog rows, rewrite logs or modify investigations. Configuration YAML and hash remain immutable; validation metadata may change. Parent row locks serialize natural-key registration retries. Existing natural keys are adopted without updating their values and audited as `RESOURCE_ADOPT_EXISTING`.

The supported distribution is **otelcol-contrib 0.135.0**, matching `infrastructure/otel/Dockerfile`. Control-plane Dockerfiles bundle that exact validator; native Java deployments set `SHIPPER_VALIDATOR_BINARY` to an absolute path to the same distribution. Validation executes a fixed argument vector with a 15-second timeout, bounded concurrency and temporary files. A missing validator reports `UNAVAILABLE`, never `VALID`. Validation output is kept out of API responses because Collector diagnostics can contain deployment details.

An operator must configure authorized destinations before registration or generation. An empty list intentionally provides no arbitrary destination entry. Create a backend-only `/etc/logatron/shipper.yml` (or set `SHIPPER_SETTINGS_FILE`) based on this example:

```yaml
logatron:
  shipper:
    validator-binary: /usr/local/bin/otelcol-contrib
    destinations:
      - id: primary-tls
        name: Primary telemetry gateway (TLS)
        endpoint: telemetry.example.invalid:4317
        insecure: false
        # Optional paths, not certificate/key contents. Provision on both validator and target.
        # ca-file: /etc/logatron/certs/ca.crt
        # cert-file: /etc/logatron/certs/client.crt
        # key-file: /etc/logatron/certs/client.key
        # Empty scope lists authorize this destination for all administered resources.
        # Restrict these lists in deployments with environment-specific destinations.
        project-ids: []
        environment-ids: []
```

Mount this file read-only into the platform-api container using a deployment-specific Compose override. Destination values and certificate references are operator-managed. No private key, password or exporter credential content is returned in previews. Explicit insecure destinations may be configured for a specific environment; TLS certificate verification is otherwise enabled. Revalidate after changing trust material. Certificate/key paths must exist where validation runs as well as on the target.

## Onboard through the frontend

1. Sign in as a global or project administrator. In **Administration**, select an existing project. Global administrators can select a company and create one. Inspect existing resources before creating anything.
2. Select or create an environment with its actual type: DEV, UAT, STAGING, PROD, TEST or OTHER.
3. Register a server using a stable host ID, hostname/IP, operating system, installation directory and Collector executable path. Installation details are catalog labels for operators; the backend never executes them as commands. Only Linux file collection is supported by this version.
4. Select or create a logical service. Select the server and create/select a service instance. The backend rejects a service/server project mismatch.
5. Register the collector identity with a stable agent key and an authorized destination. Registration enables generation/download without requiring remote credentials or an installed agent. Save the one-time token only if installing the optional pull agent; the database stores its SHA-256 hash. Dismiss the secret when finished.
6. Select the service instance and add one or more log sources. Configure name, primary path/glob, optional additional includes and excludes, parser, timezone, profile/custom/disabled multiline handling, beginning/end start position, maximum record size (up to 1MiB) and enabled state. Java pipe logs should use `JAVA_PIPE_V1`; level-first logs use `JAVA_PIPE_LEVEL_FIRST_V1`.
7. Open **Shippers** and select project, environment and server. Review source, instance, service, server and project identifiers. Click **Generate configuration**. Read all overlap warnings and inspect YAML. Configuration generation and status reads do not release a new version to any agent.
8. Click **Validate configuration**. `VALID` means the exact contrib binary accepted the immutable YAML. Target validation is still required. `UNKNOWN`, `INVALID` and `UNAVAILABLE` are distinct states.
9. Click **Download YAML** for manual installation, or follow the optional agent rollout below. A download is a snapshot, not a remote deployment. Check its validation status and validate on the target before using it.

Receiver max_log_size does not increase the processor MAX_EVENT_BYTES or MAX_STACK_TRACE_BYTES limits. Keep those limits compatible before enabling larger records.

The main pattern plus additional includes form one receiver with one resource identity. Each source has a collision-free full-UUID receiver/transform/pipeline name and independent metadata. Supported globs are absolute Linux paths using `*`, `**` and `?`; relative paths, traversal, interpolation, control characters and bracket globs are rejected. Date-wise directories such as `/logs/app/*/*.log` work directly.

When enabled sources overlap, lower source UUID wins deterministically. The generator warns and excludes the winning source's include patterns from later receivers. This prevents duplicate collection even across instances. This is conservative: files excluded by the winning source also remain reserved by that source. Prefer disjoint patterns and review the warnings; do not rely on broad overlapping sources to fill exclusion gaps.

## Optional authenticated pull deployment

There is no SSH deployer or frontend shell execution. One operator installation of the existing `tools/managed-shipper/logatron-managed-shipper.sh` is required. Download-only servers need none of these prerequisites.

On the authorized Linux server, provision contrib 0.135.0, bash, curl, Python 3, flock and timeout. Use a dedicated service user that can read only the configured log files and write only its state directory. Provision a protected service environment file with the token and paths:

```sh
LOGATRON_PLATFORM_URL=https://control-plane.example.invalid
LOGATRON_AGENT_ID=<registration agent ID>
LOGATRON_AGENT_TOKEN=<one-time registration token>
OTELCOL_BIN=/opt/logatron/otelcol-contrib
LOGATRON_STATE_DIR=/var/lib/logatron-agent
LOGATRON_POLL_SECONDS=15
```

The local state path must match the control-plane `AGENT_STATE_DIRECTORY` used in YAML. Keep that path stable across configuration versions. Its `storage/` contains receiver checkpoints and the exporter queue. Certificate paths are supplied by backend destination settings and provisioned out of band. HTTPS verifies the control-plane certificate using the host trust store; never use curl `-k`. `LOGATRON_ALLOW_HTTP=true` is an explicit development/environment exception, independent of the OTLP destination's insecure flag.

Protect the environment file (0600) and state directory (0700). The agent also uses umask 077, a local process lock and a protected curl header file. It has no inbound deployment port and does not accept commands, binary paths or service names from the frontend. Run it under the existing systemd service pattern in [the installation guide](../tools/managed-shipper/README.md), updating that service to point to this script and its environment file. Installing/restarting the service is an operator action.

The agent reports `managed-v2` and its binary version before requesting a release. Old agents receive no newly generated configurations. Existing installations must upgrade the script before using rollout controls. Deployment is disabled by default, including after the database migration.

In **Shippers**, authorize automated deployment for the selected server. This does not restart the Collector. A fresh authenticated heartbeat, exact target version, enabled deployment authorization and a `VALID` snapshot are required for **Deploy configuration**. Review the diff, confirm no parallel collector is reading the files, and confirm the release/restart. The request contains the selected version observed by the browser; a competing release returns 409 and requires review again.

The agent verifies the downloaded hash, validates with the target binary (20-second timeout), saves the previous file and its identity, atomically renames the new file, restarts its own Collector child and verifies both process liveness and the local health endpoint. Failed validation retains the old file. Failed startup/health restores the previous file and acknowledged hash/version. A matching authenticated deployment ID/hash/version and successful health check are required for `APPLIED`. Errors mark the rollout `FAILED` and restore the previously selected version in the control plane. Repeated polling of an unchanged version does not restart it.

`APPLIED` is deliberately not an end-to-end ingestion success claim. Last ingestion remains **Unknown** because the current platform has no authoritative per-shipper acknowledgement/last-ingestion telemetry API. Inspect **Log Explorer** in the matching project/environment and verify a new event before declaring operational success. Agent errors and heartbeat are shown as reported; no heartbeat or health value is synthesized. If an agent disconnects with a rollout pending, the request remains pending until it reconnects and acknowledges; do not run a second collector to force progress.

## Rollback and retirement

Select a previously generated configuration in **Shippers**, validate it, inspect its diff and choose **Roll back to this version**. Confirm the same server and restart. This creates a distinct audited `ROLLBACK` deployment selecting the original immutable snapshot; it does not rewrite resource UUIDs or delete newer snapshots. Pending competing releases are refused. Rollback destinations must still be authorized by backend policy. Re-enabling disabled sources in the catalog may be required before replaying an older snapshot because the ingestion processor resolves enabled catalog sources.

For download mode, retain the current config and state directory, validate the downloaded version using the target binary, back up the old file, atomically rename the replacement on the same filesystem and restart through the operator's approved service mechanism. On failure, stop that Collector, restore the saved file and restart it with the same checkpoints. Never run old and new versions simultaneously.

For explicit authorized retirement, first drain/stop the old Collector, then disable the affected log sources in Administration or use the existing confirmed **Decommission server** action. Generate/release a version without those sources if an agent remains installed. Disabling a catalog source causes ingestion metadata resolution to reject future events for that source; drain queued events before disabling. Historical logs and investigations stay intact. Do not delete legacy rows. Re-enable the same rows and restore the saved config/state to reverse retirement.

## Envelope, deduplication and rotation

The existing schemaVersion 1 envelope is retained. The transform creates a fresh UUID event ID and RFC3339 timestamp, stable collector key, actual database IDs, file path, parser and original string payload including stack-trace whitespace. Existing gateway/processor masking remains authoritative. User strings are serialized with YAML/JSON serializers, and Collector dollar interpolation is escaped.

`sourceOffset` is a documented compound identity: `logSourceId | log.file.path | log.file.record_offset | SHA256(original body)`. It uses the contrib receiver's real byte-offset attribute, not a path pretending to be an offset. Retries after transformation preserve that identity through the persistent queue. Replay at the same path/offset/body produces the same fingerprint inputs. Native rename/create rotation and persistent fingerprint checkpoints are supported. There is no universal inode/generation identity in this envelope: identical file replacements at the same path, byte offset and body cannot be distinguished; renamed-file replay can produce a different offset identity. Copy-truncate edge cases, external checkpoint deletion and arbitrary historical backfill do not carry an exactly-once guarantee. Do not broaden to rotated `*.log.*` files without an explicit replay plan.

The new receiver names use full source UUIDs rather than the old compiler's 12-character prefix. This avoids component collisions, but **does not migrate old Collector checkpoint keys automatically**. Plan the initial legacy cutover below. No private state database is rewritten automatically.

## Legacy UAT cutover

Select the existing project, environment, server, service, instance and source rows in Administration. Adopt their persisted IDs; do not recreate resources to bypass natural-key detection. Back up the current configuration and persistent state. Drain the old queue, pause writes if possible and stop the old Collector before activating a reviewed replacement. Full-UUID receiver names do not automatically transfer old checkpoint keys. Use end-start with a controlled write pause, or perform a separately reviewed checkpoint migration. Keep the saved configuration and matching state for rollback. Never run two collectors on the same files.

## API

All public endpoints below are under `/api/v1/admin`, require JWT authentication and derive administration scope from persisted membership. Unauthorized resource lookups are non-enumerating 404 responses. Existing catalog registration endpoints remain unchanged except additive `shipperOptions` on source requests/responses.

| Method | Route | Purpose |
|---|---|---|
| GET | `/servers/{serverId}/shipper-destinations` | Authorized endpoint names and TLS mode, without secret material |
| POST | `/collector-agents` | Existing identity registration; endpoint must now be backend-approved |
| POST | `/shipper-configurations/generate` | `{agentId}`; idempotent immutable snapshot, no activation |
| GET | `/collector-agents/{agentId}/shipper` | Configuration/deployment history, selection and prerequisites |
| GET | `/collector-agents/{agentId}/config-revisions/{version}` | Inspect one immutable YAML version |
| POST | `/collector-agents/{agentId}/config-revisions/{version}/validate` | Exact binary validation; no restart |
| GET | `/collector-agents/{agentId}/config-revisions/{version}/download` | `config.yml` attachment; no-store |
| PUT | `/collector-agents/{agentId}/deployment-settings` | `{enabled, confirmed:true}`; authorize future releases |
| POST | `/collector-agents/{agentId}/deployments` | `{version, expectedSelectedVersion, confirmed:true}`; release with restart authorization |
| POST | `/collector-agents/{agentId}/rollback` | Same body; explicit audited rollback |

Source options example (additional to existing source fields):

```json
{"shipperOptions":{"name":"Application logs","startAt":"end","maxLogSize":"200KiB","includes":[],"excludes":[]}}
```

The token-authenticated internal endpoints remain `/api/internal/v1/agents/{agentId}/config` and `/heartbeat`, with `X-Logatron-Agent-Token`. Config returns 204 before a selected release or for an old agent protocol, otherwise immutable YAML and hash/version/deployment-ID headers. The heartbeat adds `protocol:"managed-v2"`, `deploymentId` and `healthVerified`. Tokens cannot administer resources or generate/release arbitrary versions.

Validation errors use the existing ApiError format; conflicts return 409. A target failure is acknowledged as `FAILED`, while health-verified application is `APPLIED` with ingestion uncertainty explicitly recorded.

## Verification

Run `backend/mvnw verify`, frontend `pnpm build` / `pnpm test --watch=false`, and `bash tools/managed-shipper/test-managed-shipper.sh`. Docker-enabled integration tests create disposable PostgreSQL and contrib containers. Default contrib image is pinned at 0.135.0; local verification can override `-Dtest.otel.image` with the repository's locally built image after verifying its version. Tests do not use any company host or deployment destinations.

`ShipperApiIntegrationTest` covers full database-backed registration, real generated configuration delivery through a disposable OTLP gateway, ID/Java multiline assertions, restart/rollback checkpoints, concurrent idempotency, access denial, adoption and rollout failures. `CollectorRuntimeIntegrationTest` independently validates real contrib behavior. Existing processor tests cover Kafka → masking/canonicalization → ClickHouse. The new gateway runtime test captures the OTLP endpoint output; it does not claim a single all-services Compose smoke test. Frontend tests cover onboarding, preview, download, diff, confirmations, stale scope responses and errors. The shell fixture covers deployment operations without contacting any server.
