# Development Guide

## Repository layout

```text
backend/common-contracts/   versioned ingestion records and JSON schemas
backend/platform-api/       Phase 1 control plane and PostgreSQL migrations
backend/log-processor/      Phase 2 Kafka consumer/canonicalization worker
frontend/                   Angular standalone application
infrastructure/otel/        pinned agent/gateway configuration and image
infrastructure/clickhouse/  forward-only ClickHouse migrations
tools/log-generator/        deterministic file log scenarios
docs/                       ADRs and ingestion runbook
```

## Boundaries

The platform API keeps controller → application → domain/port → adapter direction. The processor keeps parsing independent of transport/persistence; masking, validation, deduplication, and enrichment do not depend on Kafka. ArchUnit enforces cycles and these boundaries.

`common-contracts` owns `RawLogEnvelope`, `CanonicalLogEvent`, `DeadLetterEvent`, `ParserProfile`, and their schemas. Changes must be additive/backward-compatible or use a new schema/topic version.

## Ingestion invariants

- PostgreSQL `log_sources` metadata is authoritative; envelope IDs are untrusted claims and must match the cached source scope.
- Gateway masking is baseline protection; processor masking is authoritative and runs before persistence or fan-out.
- DLQ creation re-redacts its preview and never includes an unbounded raw payload.
- Canonical validation runs after metadata enrichment and masking.
- Kafka auto-commit is disabled. Batch acknowledgment is last, after ClickHouse and all required topic sends.
- ClickHouse failure throws through the listener. The error handler retries with backpressure; it must not skip or acknowledge the batch.
- At-least-once delivery permits reprocessing. Stable event IDs/fingerprints and `ReplacingMergeTree` handle duplicates; do not claim exactly-once delivery.
- Liveness reports process viability. Readiness includes ClickHouse so an outage does not cause a restart loop.

## Migrations

PostgreSQL Flyway migrations are under `backend/platform-api/src/main/resources/db/migration`. ClickHouse migrations are under `infrastructure/clickhouse/migrations` and run through the one-shot Compose service. Applied files are immutable; add a new ordered migration.

## Tests

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
corepack pnpm test
corepack pnpm build
```

The backend command runs contract compatibility, Phase 1 unit/security/PostgreSQL integration, processor parser/security/reliability tests, listener no-ack failure semantics, ArchUnit rules, and a real Kafka/PostgreSQL/ClickHouse Testcontainers pipeline.

Collector configs can be validated without dependencies:

```powershell
docker compose run --rm --no-deps otel-agent validate --config=/etc/otelcol/config.yaml
docker compose run --rm --no-deps otel-gateway validate --config=/etc/otelcol/config.yaml
docker compose config --quiet
```

## Adding a parser profile

1. Add a versioned `ParserProfile` value only when routing cannot be represented by an existing profile.
2. Implement a bounded parser with timestamp and exception tests.
3. Add source metadata through a forward PostgreSQL migration.
4. Add generator fixtures for success, malformed input, Unicode, missing IDs, and secret-shaped content.
5. Prove canonical validation, redacted DLQ behavior, and full Docker flow.

## Security

Never log payloads or secrets from parsing failures. Metric tags must remain bounded and non-sensitive. Production deployments require TLS/SASL, external secret management, non-local credentials, replicated Kafka/ClickHouse, and policy-approved masking rules; the Compose stack is a single-node development proof.