# Shipper implementation audit

The existing application is Angular 21 / PrimeNG with a Spring Boot 3.5 control plane, PostgreSQL/Flyway, and a Kafka → Java processor → ClickHouse ingestion pipeline. No separate application is needed.

* `/administration` already creates projects, environments, servers, services, instances and sources through `/api/v1/admin/*`. `/projects`, `/logs`, `/investigations`, `/traces` and existing scope/RBAC remain in place.
* Catalog relationships are company → project → environment → server, with project-owned services and server-owned instances. Sources belong to instances. Natural-key unique constraints already prevent duplicate registrations. Source metadata and investigation references must retain their UUIDs.
* `RawLogEnvelope` v1 accepts a structured OTLP body. The gateway exports raw envelopes to Kafka. `MetadataResolver` resolves enabled persisted sources; `Fingerprinter` includes sourceOffset in deduplication. A file path alone is not a record offset.
* `CollectorConfigCompiler` and immutable `collector_config_revision` already exist. Previously, status reads and agent polling compiled revisions and immediately made them deployable. Generation, review and explicit deployment must be separate.
* `tools/managed-shipper` is the existing token-authenticated pull agent (hashed tokens). There is no SSH credential store. Extend this agent with explicit rollout authorization, TLS transport, validation, bounded operations and rollback; do not introduce SSH.
* `infrastructure/otel/Dockerfile` pins contrib **0.135.0**. Remote binary versions are unknown until reported. Generated configurations must be checked with that exact distribution/version, and the target must independently validate before activation.
* Historical legacy UAT resources are deployment data, not repository seed data. Adoption selects those existing rows; no migration should insert example UUIDs, rewrite keys, delete logs or run against the remote server.

Implementation adds source options, explicit immutable snapshot generation/validation/download, rollout history and selection, backend-managed destinations, and a native shipper view linked to existing onboarding. Unknown ingestion and health remain unknown. Unsupported deployment prerequisites fail closed.
