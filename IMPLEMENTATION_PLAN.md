# LOG-A-TRON Implementation Plan

## 1. Delivery objective

Deliver LOG-A-TRON incrementally, proving the complete path from a host log to an authorized cross-service search before adding investigation automation. Each milestone must be runnable, observable, documented, and testable. A milestone is complete only when its acceptance criteria pass; future-phase scaffolding does not count as delivered capability.

The V1 product increment is complete when a developer can:

1. authenticate and see only permitted projects and environments;
2. filter centrally collected logs by project, environment, server, service, level, and bounded time range;
3. search by trace ID, request ID, correlation ID, transaction ID, order token, or auction ID;
4. open surrounding events and follow an identifier across services and hosts;
5. view a related trace when tracing is available; and
6. trust that sensitive fields are masked and every sensitive search/export is audited.

## 2. Guiding delivery rules

- Prefer vertical slices over building every database or UI layer independently.
- Keep `main` runnable; use Flyway/versioned ClickHouse migrations for every schema change.
- Define and compatibility-test contracts before producers and consumers evolve independently.
- Build and test after each meaningful change.
- Use representative generated incidents, not only happy-path fixtures.
- Add infrastructure only when a delivered feature consumes it.
- Establish performance and security budgets early; do not postpone tenant isolation tests.
- Record significant decisions as short ADRs in `docs/adr/`.

## 3. Proposed toolchain and baseline

- Current LTS Java and a supported Spring Boot 3.x release selected at implementation time.
- Maven reactor with Maven Wrapper and reproducible dependency/plugin versions.
- Angular current supported release, strict TypeScript, standalone components, PrimeNG, and a locked Node package manager version.
- PostgreSQL with Flyway; ClickHouse with ordered SQL migration jobs.
- Apache Kafka in KRaft mode; official clients and Spring for Apache Kafka.
- Testcontainers for Kafka, PostgreSQL, and ClickHouse integration tests.
- OpenAPI generated from backend annotations/contracts; frontend API client generated during build.
- JUnit 5, AssertJ, Mockito only at genuine isolation boundaries, ArchUnit or Spring Modulith verification, and Playwright for critical UI flows.
- OpenTelemetry Java agent/SDK and Micrometer bridge where appropriate.

Exact versions are intentionally not frozen in an architecture document; they must be selected from supported versions and recorded in the parent POM/package manifest when implementation starts.

## 4. Work breakdown

### Phase 0 — Decisions and measurable requirements

Purpose: remove production-critical ambiguity before infrastructure is treated as final.

Deliverables:

- Event-volume worksheet: average/peak events per second, record sizes, burst duration, current retention, growth.
- SLO/RPO/RTO proposal and failure-budget definitions.
- IdP integration decision and role/group mapping.
- Data classification, PII/secret masking, audit, export, and legal-hold requirements.
- Network, TLS/mTLS, secret manager, and production ownership decisions.
- ADRs for modular monolith, OTel Collector, ClickHouse sort key, Kafka semantics, and trace backend.

Exit criteria:

- Security and platform owners approve the scope and threat boundaries.
- Representative log samples are available in a controlled, sanitized corpus.
- Load targets and maximum acceptable ingestion/search latency are documented.

### Phase 1 — Foundation and control plane (complete)

Purpose: establish build, metadata, security, and local runtime foundations without pretending ingestion exists.

Backend:

- Create Maven reactor: `common-contracts`, `platform-api`, and `log-processor`.
- Add Spring Boot configuration profiles, structured errors, validation, OpenAPI, health/readiness, and OTel self-instrumentation.
- Implement catalog modules and Flyway migrations for company, project, environment, server, service, instance, and log source.
- Implement OIDC/JWT resource server and a development-only OIDC profile or signed test-token mechanism.
- Implement effective-scope authorization and role/permission model.
- Implement append-only audit application service and initial audit migration.
- Add authorized catalog/admin REST APIs.

Frontend:

- Create Angular workspace, application shell, OIDC flow, API error handling, scope picker, and guarded routes.
- Implement project/environment/server/service inventory views needed to validate permissions.

Infrastructure:

- Add Compose services for PostgreSQL, backend, and frontend only.
- Add `.env.example`, health checks, persistent volume, seed migration, and startup documentation.

Tests:

- Unit tests for permission intersections and role/environment behavior.
- Integration/API tests proving users cannot enumerate or mutate another project.
- Flyway clean-migrate validation and repository tests.
- Architecture-boundary test preventing forbidden module dependencies.

Exit criteria:

- A user can authenticate, see exactly their permitted catalog, and administer resources only with the required permission.
- All allow/deny decisions in the test matrix pass and denied resource lookup does not leak existence.
- A clean checkout starts through the documented local command.

### Phase 2 — Ingestion vertical slice (complete)

Purpose: reliably convert generated host logs into queryable structured ClickHouse rows.

Contracts and Kafka:

- Define JSON/Avro decision and versioned `RawLogEnvelope`, `CanonicalLogEvent`, and `DeadLetterEvent` contracts. JSON Schema is sufficient initially if compatibility CI is enforced.
- Provision `logs.raw.v1`, `logs.enriched.v1`, `logs.errors.v1`, and `logs.dlq.v1` idempotently with explicit partitions, replication, and retention.
- Define stable event ID, source offset, retry headers, and keying rules.

Collector:

- Add host-agent and gateway OTel configurations.
- Generate agent filelog receiver mappings from seed log sources.
- Configure multiline stack traces, file rotation/checkpoints, batching, memory limiting, disk queue/retry, resource enrichment, and baseline masking.
- Add collector configuration validation in CI.

Processor:

- Implement parser-profile selection, JSON parser, configurable plaintext parser, timestamp/level normalization, exception extraction, and service mapping.
- Implement authoritative PII/secret masking before persistent storage.
- Validate size/schema, generate fingerprints, handle duplicates, and redact DLQ payloads.
- Insert ClickHouse batches and commit Kafka offsets only after acknowledgement or durable DLQ publication.
- Add readiness tied to configuration and dependency state without failing liveness on a temporary downstream outage.

Storage:

- Add versioned ClickHouse migration runner, `logs_local`, and minute aggregate view.
- Implement insert adapter with bounded batch size, retry classification, and metrics.

Sample generator:

- Generate Demo Marketplace, Demo Payments, Demo Analytics, and Demo Support traffic across the named services.
- Support deterministic seeds and scenario IDs.
- Implement normal traffic plus DB pool exhaustion, Kafka retry/duplicate key, HTTP timeout, and null-pointer scenarios.
- Emit shared trace/business identifiers and deliberate malformed/missing-ID/out-of-order/duplicate events.

Tests:

- Parser and masker golden-file tests including large/multiline and Unicode records.
- Testcontainers end-to-end test: Kafka input to ClickHouse row.
- Duplicate delivery, poison event, DLQ redaction, ClickHouse outage/recovery, and offset-commit tests.
- Collector rotation and checkpoint restart test.
- Contract compatibility tests.

Exit criteria:

- Generated events are visible as canonical ClickHouse rows with correct tenant/source metadata.
- Injected secrets do not appear in Kafka persistent topics, ClickHouse, DLQ, processor logs, or test artifacts.
- Restarts and a bounded ClickHouse outage produce no acknowledged-event loss; duplicates remain within the documented convergence semantics.
- Ingestion/DLQ/lag dashboards or metrics expose pipeline health.

### Phase 3 — Secure Log Explorer (V1 core)

Purpose: deliver the highest-priority useful product without AI.

Backend:

- Implement `LogSearchCriteria` with scope, time, level, identifiers, exception type, and safe text terms.
- Implement authorization-first controlled ClickHouse query builder.
- Add cursor paging, timeout, scanned-byte/result limits, query cancellation, and concurrency/rate controls.
- Add search, event detail, surrounding context, correlation, live SSE, saved search, and overview aggregation APIs.
- Exclude stack trace/body from search rows; fetch detail separately.
- Record query digest/scope/outcome and export actions in audit events.
- Expose trace link only when trace ID exists and the caller can access the event scope.

Frontend:

- Build cascading project/environment/server/service selectors and reusable UTC/local time-range picker.
- Build virtualized log table with level, timestamp, service/host, message preview, and correlation badges.
- Add expand/detail and canonical JSON views, stack trace rendering, copy-with-policy, context window, and saved searches.
- Add identifier chips to pivot across services and a “view trace” action.
- Implement best-effort live tail with pause/resume, buffer cap, reconnection state, and clear distinction from persisted results.
- Provide URL-safe shareable search references using saved-search IDs or validated compact filters; never put tokens/secrets in URLs.

Tests:

- API tests for every filter, pagination boundary, invalid range, timeout, and empty result.
- Authorization tests that attempt cross-project and unauthorized PROD searches through every endpoint, including context/live/detail.
- ClickHouse query-plan and representative-volume benchmarks.
- Playwright flow from login to cross-service correlation.
- Accessibility checks for keyboard navigation, focus, contrast, and screen-reader labels.

Exit criteria:

- The full V1 success criterion passes with generated multi-host incidents.
- p95 search meets the agreed target at the representative dataset size.
- No endpoint can retrieve an event outside effective scope by guessing IDs.
- UI never performs an unbounded query and clearly reports timeout/partial availability conditions.

### Phase 4 — Distributed tracing

Purpose: correlate logs with end-to-end service flow.

- Add Tempo, Prometheus, and Grafana through an optional Compose observability profile.
- Configure gateway trace routing and retention.
- Instrument sample Java HTTP, Kafka producer/consumer, and JDBC flows with the OTel Java agent and limited custom attributes.
- Enforce W3C context propagation and allowlisted Kafka/business headers.
- Implement trace query adapter and `/api/v1/traces/{traceId}` with backend authorization based on mapped project resource attributes and related log scope.
- Build a basic Angular trace waterfall/span detail and related-log pivot, or deep-link to Grafana initially if custom visualization does not yet add value.
- Add broken-context and mixed-authority tests.

Exit criteria:

- A generated request is followed through HTTP, Kafka, settlement, and database spans.
- Clicking a span finds permitted related logs; clicking a log opens its trace.
- A trace lacking trustworthy project metadata fails closed or exposes only safely authorized evidence.

### Phase 5 — Deterministic investigation engine — Complete

Purpose: reconstruct reliable evidence before natural-language or LLM features.

- Define `InvestigationQuery`, evidence references, timeline events, confidence/quality fields, and provenance.
- Implement correlation by trace, request, transaction, order, auction, and Kafka message identifiers.
- Account for clock skew, ingestion delay, missing IDs, retries, duplicates, and out-of-order events.
- Implement first-failure analysis, exception fingerprint clustering, dependency graph traversal, error-rate comparison, and impact counts.
- Store investigation metadata and evidence references in PostgreSQL, not copied raw logs.
- Build investigation UI with timeline, affected services, evidence links, and explicit unknowns.

Exit criteria:

- Controlled scenarios produce deterministic timelines with expected first failure and downstream symptoms.
- Every conclusion links to a still-authorized log, trace, or metric reference.
- Quality degrades explicitly when correlation or timing data is incomplete.

### Phase 6 — Metrics correlation and platform health — Complete

Purpose: add measured infrastructure/application evidence and operational alert prerequisites.

- Add Prometheus production configuration and OTel metrics routing.
- Collect JVM, HTTP, DB pool, Kafka, host, collector, processor, API, and ClickHouse metrics with cardinality controls.
- Add a metric query port with allowlisted queries and authorized project/service mapping.
- Align metric windows with timeline events and calculate evidence such as DB pool saturation preceding errors.
- Complete operational dashboards and alerts for ingestion stopped, lag, DLQ, collector offline, storage pressure, and query SLOs.

Exit criteria:

- The DB-pool scenario shows pool saturation before settlement errors and later Kafka lag.
- Metric evidence references its exact series/window and does not rely on LLM interpretation.

### Phase 7 — AI provider boundary — Complete

The repository implementation for this phase is a deterministic template stub. It validates the bounded provider contract and failure behavior but does not integrate with or call an external LLM.

Purpose: explain bounded deterministic evidence without granting the model data-plane control.

- Define replaceable `AiProvider`, structured request/output schema, timeouts, budgets, and provider policy.
- Implement natural-language intent classification into the sealed controlled query set.
- Require user confirmation for ambiguous scope/time before expensive investigation.
- Build bounded evidence packages with token/record caps, masking, ranking, provenance, and data classification policy.
- Validate output and enforce fact/evidence/inference/hypothesis labels and evidence citations.
- Store provider/model/prompt-template versions, evidence references, latency, and outcome audit metadata without retaining restricted prompt bodies by default.
- Add prompt injection, data exfiltration, unsupported conclusion, timeout, and provider outage tests.

Exit criteria:

- AI explanations cite deterministic evidence and cannot execute SQL or broaden scope.
- Removing or failing the AI provider leaves the deterministic investigation product usable.
- Evaluation scenarios meet agreed groundedness and unsupported-claim thresholds.

### Phase 8 — Knowledge and RAG

Purpose: retrieve relevant internal guidance and similar incidents.

- Add pgvector migration only now; ingest approved, versioned runbooks, postmortems, known errors, and architecture documents.
- Preserve source ACLs and classification through chunking, embedding, retrieval, and evidence packaging.
- Implement hybrid metadata/text/vector retrieval and similar-incident evaluation.
- Link recommendations to exact source/version and distinguish knowledge guidance from observed incident facts.
- Add deletion/re-index, stale-document, and revoked-access tests.

Exit criteria:

- Retrieval never crosses user/document ACLs.
- Suggested runbooks and incidents have measurable relevance on a curated evaluation set.

### Phase 9 — Retention, archive, incidents, and alerts

Purpose: add governance and response workflows after the data path is stable.

- Implement retention policy precedence, legal holds, verified Parquet export manifests, encryption, MinIO/S3 lifecycle, and deletion audit.
- Implement incident lifecycle and immutable evidence references.
- Implement alert rules for spikes, new exceptions, lag, offline collectors, ingestion gaps, and critical patterns.
- Add notification provider interfaces; implement one provider only when credentials, ownership, deduplication, escalation, and delivery audit are defined.
- Test archive verification, restore/search workflow, legal holds, alert deduplication, and provider retries.

Exit criteria:

- Retention simulations prove the correct policy wins and unverified data is never deleted.
- Alerts link to a scoped search/investigation and do not disclose sensitive payloads in notifications.

### Phase 10 — Production hardening

Purpose: validate the system under realistic security, failure, and scale conditions.

- Threat model and independent security review.
- Load, soak, burst, and noisy-neighbor tests with production-like record sizes.
- Kafka partition/replication, ClickHouse shard/replica/backup, and PostgreSQL HA/backup plans.
- Restore and disaster-recovery rehearsal with measured RPO/RTO.
- Query tuning from real workload; add projections/lookup tables only when justified.
- Dependency, SBOM, image signing/scanning, patching, and secret-rotation procedures.
- Runbooks, capacity alerts, on-call ownership, upgrade/rollback strategy, and release gates.
- Kubernetes/managed-service deployment design if the production platform requires it.

Exit criteria:

- Security findings are resolved or risk-accepted by accountable owners.
- SLOs and recovery objectives pass rehearsals.
- On-call can diagnose pipeline, storage, query, and authorization failures using documented runbooks.

## 5. V1 API and UI delivery slices

To avoid a backend-first waterfall, Phase 3 should be delivered in these thin slices:

1. Authorized default 30-minute search for one project/environment.
2. Service/server/level filters and cursor pagination.
3. Event detail with separately loaded stack trace and JSON.
4. Trace/request/correlation/business-ID pivot across services.
5. Surrounding logs and saved searches.
6. Aggregated overview and bounded live tail.
7. Export only if governance requirements and permission/audit controls are approved.

Each slice includes backend contract, authorization tests, UI, telemetry, documentation, and representative generated data.

## 6. Test strategy

### Test pyramid

- Unit: parsing, normalization, masking, scope intersection, query validation, fingerprinting, timeline ordering.
- Module/component: Spring application services with real module boundaries and fake external ports.
- Integration: PostgreSQL, Kafka, ClickHouse, and later Tempo/Prometheus through Testcontainers or isolated Compose.
- Contract: schema compatibility, OpenAPI compatibility, Kafka consumer/provider fixtures.
- End-to-end: generated scenario from collector/file through UI search and trace pivot.
- Non-functional: authorization fuzzing, load/soak, failure injection, recovery, accessibility, and secret scanning.

### Mandatory scenario matrix

| Scenario | Required assertion |
|---|---|
| Duplicate logs / Kafka redelivery | stable IDs; documented dedup behavior; no multiplied overview count after convergence |
| Out-of-order logs / clock skew | deterministic ordering with observed-time evidence and uncertainty surfaced |
| Missing trace ID | searchable by other identifiers; no fabricated trace relationship |
| Collector retry/restart/rotation | persisted offsets; no acknowledged loss; bounded duplicates |
| ClickHouse unavailable | Kafka lag rises, processor backpressures, recovery drains backlog |
| Malformed or oversized log | redacted bounded DLQ event; partition continues |
| Large stack trace | storage/API cap and explicit truncation metadata |
| Unauthorized project or PROD scope | 403/404 policy response and no rows, counts, live events, or existence leakage |
| PII/secret in JSON/message/stack trace/DLQ | value masked at every durable and response boundary |
| High-volume/noisy tenant | quotas preserve platform health and other tenant latency |
| Schema upgrade during backlog | old and new envelopes remain processable during migration window |

## 7. CI/CD and quality gates

Every pull request should run:

- formatting, static analysis, dependency and secret scans;
- Java/TypeScript unit tests and architecture-boundary tests;
- Flyway migration validation and ClickHouse migration smoke test;
- Kafka schema backward-compatibility checks;
- integration tests for changed adapters;
- Angular build, component tests, and critical Playwright smoke tests;
- container image build and vulnerability scan when deployables change;
- documentation link/Mermaid validation where supported.

Release candidates additionally run end-to-end, authorization suite, performance regression subset, Compose clean-start/upgrade, and rollback/migration rehearsal. Database migrations are forward compatible with the previous application version during rolling deployment.

## 8. Operational readiness checklist

Before a production pilot:

- Ownership and on-call rotations are explicit for collectors, Kafka, ClickHouse, PostgreSQL, and the product.
- Dashboards cover ingestion rate, end-to-end lag, DLQ, drops, collector config drift, storage/merge pressure, and query SLOs.
- Alerts have actionable runbooks and avoid high-cardinality or sensitive labels.
- Backup/restore, certificate expiry, secret rotation, and dependency upgrade procedures are tested.
- Capacity includes a documented Kafka/disk buffer window for downstream outage.
- Tenant onboarding/offboarding and access review processes are documented and audited.
- Masking-rule changes are versioned, tested against a corpus, and deployable before new sources are enabled.
- Data retention, legal holds, exports, and incident evidence comply with company policy.

## 9. Documentation deliverables

Maintain alongside code:

- `README.md`: purpose, status, quick start, and links.
- `SETUP.md`: prerequisites, configuration, Compose profiles, seed data, troubleshooting.
- `DEVELOPMENT.md`: module rules, build/test/debug workflows, migrations, contract evolution.
- `API.md`: API conventions, authentication, paging, limits, examples, OpenAPI generation.
- `SECURITY.md`: threat model, roles/scopes, masking, secrets, auditing, incident reporting.
- `OBSERVABILITY.md`: telemetry, dashboards, SLOs, alerts, and operational runbooks.
- `AI-RCA.md`: deterministic evidence contract, model boundary, evaluations, and safety controls (Phase 7).
- `docs/adr/*.md`: important decisions and consequences.

## 10. Initial backlog order

The first implementation iteration after plan approval should use this order:

1. Confirm Phase 0 decisions and create ADRs.
2. Scaffold Maven/Angular builds and Compose PostgreSQL.
3. Implement catalog schema and permission/scope evaluator with tests.
4. Expose authorized catalog APIs and scope picker UI.
5. Add Kafka/ClickHouse and canonical contracts.
6. Implement one JSON log vertical slice from generator to ClickHouse.
7. Add masking/DLQ/retry/dedup and plaintext/multiline profiles.
8. Implement one authorized search API and minimal log table.
9. Expand filters, correlation, context, saved search, and live tail.
10. Validate V1 acceptance, load, failure, and security criteria before tracing or AI work begins.

## 11. Risks requiring early spikes

Short, time-boxed spikes should answer these before committing to detailed implementation:

- Verify the chosen OTel Collector distribution/exporter can publish the required Kafka envelope and persist file offsets under rotation/restart. If not, define the smallest supported adapter.
- Benchmark the proposed ClickHouse sort key and bloom indexes with representative cardinality, record size, and identifier queries.
- Test how immediate duplicate visibility behaves with `ReplacingMergeTree`; document the query strategy without routine `FINAL`.
- Confirm OIDC group/claim sizes and the design for resolving large project memberships server-side.
- Measure multiline and maximum stack-trace behavior through collector, Kafka, processor, ClickHouse, and API caps.
- Validate ClickHouse JDBC/native client cancellation, timeout, and rows/bytes-scanned observability.

A spike produces a decision, benchmark/test artifact, and ADR; it must not become an unmaintained parallel implementation.

## 12. Critical complexity review

The requested long-term platform is broad. The plan removes or postpones the following unnecessary early complexity:

- No control-plane microservice per domain; a modular monolith provides transactional consistency and simpler operations.
- No second search engine alongside ClickHouse until a measured query requirement cannot be met safely.
- No pgvector, MinIO, Tempo, Prometheus/Grafana, or LLM service in the default first Compose milestone; optional profiles appear when consumed.
- No custom ML anomaly platform in early investigation. Statistical baselines and deterministic comparisons come first.
- No general query DSL or model-generated SQL. A small sealed operation set covers product needs safely.
- No promise of global event ordering or exactly-once delivery. The design models at-least-once and imperfect clocks honestly.
- No copying raw evidence into PostgreSQL investigation/audit records.
- No premature distributed ClickHouse topology for local development, and no production topology chosen without volume/SLO data.

The remaining complexity—Kafka, ClickHouse, PostgreSQL, OTel Collector, two backend deployables, and Angular—is justified by distinct requirements: durable buffering, analytical storage, transactional metadata, standardized collection, independently scalable processing, and the developer workflow.

## 13. Definition of V1 done

V1 is done only when all statements below are true:

- Clean local setup is reproducible from documentation and health checks become ready.
- At least four projects, multiple environments/hosts/services, and controlled failure scenarios are generated.
- File rotation, temporary network/ClickHouse failure, processor restart, duplicates, malformed records, and large stack traces pass tests.
- Every durable event is canonical, source-attributed, and masked; failures are redacted and diagnosable in the DLQ.
- Search supports all declared filters with enforced range/page/query limits and representative p95 performance.
- Trace/correlation/request/transaction/order/auction identifiers can pivot across permitted services.
- Project/environment/service isolation is enforced and covered by negative tests across every data endpoint and SSE stream.
- Search, detail, context, live access, export (if enabled), and administration actions are audited without copying sensitive bodies.
- Platform ingestion, lag, drop, DLQ, storage, and API/query health are observable.
- AI/RAG/automated RCA are absent from the critical path and cannot weaken deterministic behavior.

Only after this definition is met should the program claim a production-oriented logging V1 or begin relying on higher-level AI investigation features.
