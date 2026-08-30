# Phase 2 Ingestion Runbook

## Pipeline

```text
filelog receiver → OTLP agent queue → OTLP gateway redaction → Kafka logs.raw.v1
→ manual-ack batch processor → PostgreSQL metadata cache → parse/normalize/mask/validate/fingerprint
→ ClickHouse → enriched/errors/DLQ topics → Kafka acknowledgment
```

The agent watches `/logs/*/*.log`, persists file identity/offset state, and uses a start-pattern recombine rule so an outer JSON envelope is one event even when its embedded plaintext exception contains newline characters. The gateway redacts common bearer/password/token/secret patterns, parses the outer envelope to a map, and uses the Kafka raw encoding so the topic contains a JSON object rather than a JSON-quoted string.

## Topics

| Topic | Partitions | Local retention | Purpose |
|---|---:|---:|---|
| `logs.raw.v1` | 6 | 72 hours | durable redacted ingestion envelope |
| `logs.enriched.v1` | 6 | 72 hours | canonical fan-out |
| `logs.errors.v1` | 3 | 24 hours | WARN/ERROR/FATAL canonical events |
| `logs.dlq.v1` | 3 | 14 days | bounded redacted failures |

All use `cleanup.policy=delete`, replication factor 1 locally, explicit creation, and auto-creation disabled.

`logs.raw.v1` has a verified null Kafka key in the Phase 2 OTel v0.135.0 pipeline. That exporter can hash the complete OTel resource-attribute map, but cannot cleanly derive the literal `projectId:serviceId` key from the envelope. Keyless raw records consequently have no project/service partition affinity or per-service ordering guarantee. The authoritative IDs remain in the envelope, and processor-produced enriched/error/DLQ keys remain as designed. This is an accepted raw-boundary deviation; adding a custom adapter or brittle transform solely for key construction is out of scope.

## Canonical processing order

1. Decode and validate the outer envelope/version/size.
2. Resolve the PostgreSQL source cache and reject unknown or scope-mismatched metadata.
3. Route by authoritative parser profile (`JSON` or Java-style `PLAINTEXT`).
4. Normalize UTC timestamps, severity, trace/request/business identifiers, and exception chains.
5. Enrich authoritative company/project/environment/server/service names and IDs.
6. Apply authoritative masking to message, stack, and attribute values.
7. Validate required canonical fields and bounds.
8. Compute a stable SHA-256 fingerprint and record recently observed duplicates.
9. Insert bounded JSONEachRow batches into ClickHouse.
10. Publish enriched/error/DLQ records and acknowledge the Kafka batch last.

## ClickHouse

`logs_local` is a monthly-partitioned `ReplacingMergeTree(ingested_at)` with the architecture sort key, a 90-day local TTL, skip indexes for identifiers/text, nullable optional IDs, and Map attributes. `log_counts_1m` plus its materialized view provides minute aggregates. Duplicate collapse is eventual; physical insert metrics and final row counts can differ.

## Failure semantics

- Parser/canonical poison records become `DeadLetterEvent` records with failure stage/code, bounded reason, redacted preview, and SHA-256 payload hash.
- ClickHouse errors fail the entire listener call before fan-out/ack. Spring Kafka retries indefinitely with a one-second backoff, pausing progress on affected partitions and naturally exposing lag.
- Kafka fan-out failures also prevent acknowledgment. A retry can repeat a ClickHouse insert; stable event IDs make this safe under the documented at-least-once model.
- Collector queues/checkpoints are disk-backed. On agent restart, previously checkpointed file records are not replayed; new appends continue. Renamed active files and their replacements are tracked by file identity; the `--rotate-every` generator drill verified ten of ten records across rotation.

## Metrics and probes

Metrics use the `logatron.processor.*` namespace: `received`, `parsed`, `normalized`, `masked`, `rejected`, `dlq`, `duplicates`, `stored`, `clickhouse.failures`, `batch.size`, and `clickhouse.insert.duration`. Spring Kafka client metrics expose consumer/fetch/commit/lag measurements through Actuator. No payload or identifier values are metric labels.

- `/actuator/health/liveness`: process viability only.
- `/actuator/health/readiness`: readiness state plus ClickHouse reachability.
- `/actuator/metrics`: metric catalog.

## Verified local baseline

The deterministic 80-record `all` run yields 80 raw records, 72 stored/enriched deliveries, 40 error-topic deliveries, 8 redacted DLQ records, and 8 duplicates. After `ReplacingMergeTree` merges, 64 unique event IDs remain. Two exception types each produce eight single canonical events with non-empty multiline stacks. Known secret literals have count zero in raw Kafka and ClickHouse.

A verified outage drill added 20 raw events while ClickHouse was stopped: liveness stayed UP, readiness returned 503, the two affected partitions retained 20 uncommitted records, and `clickhouse.failures` increased. After restart, lag returned to zero, readiness returned UP, stored deliveries advanced by 20, and no acknowledged event was lost.