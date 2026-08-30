# Deterministic investigations and AI RCA

## Runtime flow

An authenticated caller submits a bounded scope, time window, and supported identifier to `POST /api/v1/investigations`. The backend resolves the caller's effective project/environment authorization before any log, trace, or metric query. A sealed planner maps natural-language input only to supported query fields; it cannot generate SQL or PromQL.

The deterministic engine correlates authorized ClickHouse logs, Tempo spans, and allowlisted Prometheus series. It orders evidence by event time, collapses duplicate event IDs, identifies one earliest supported failure, groups normalized exception fingerprints, builds trace dependency edges, labels later retries/duplicates/lag as downstream symptoms, and reports missing-ID or ordering uncertainty explicitly.

PostgreSQL stores the investigation scope, query digest, status, evidence references, quality metadata, and AI provider/model/outcome/latency. It does not copy raw logs, trace bodies, metric payloads, or prompt bodies.

## Supported identifiers

`TRACE_ID`, `REQUEST_ID`, `CORRELATION_ID`, `TRANSACTION_ID`, `ORDER_TOKEN`, `AUCTION_ID`, and an authorized `SCOPE` query are supported. Investigation windows must be positive and no longer than 24 hours.

## Metric boundary

`MetricQueryPort` exposes a sealed `MetricOperation` set. The Prometheus adapter builds only hard-coded, project-scoped templates with an optional environment scope. The initial causal scenario uses Hikari active/max connections to derive DB-pool utilization and Kafka consumer records lag as a later downstream symptom.

Project, environment, service, and scenario are bounded labels. Business identifiers such as order token, request ID, and trace ID are never metric labels. Prometheus scrapes the processor application metrics and the OTel gateway's signal exporter. The platform API actuator remains authenticated and is intentionally not scraped by the local unauthenticated Prometheus service.

## AI boundary

`AiProvider` is replaceable. It receives only the configured top-ranked evidence references and a bounded deterministic projection. Output must use the structured RCA contract and every cited evidence ID must exist in the package. Malformed responses, unsupported citations, provider exceptions, or outages yield `UNAVAILABLE_OR_REJECTED`; the deterministic result is still returned and persisted.

The implementation shipped here is `DeterministicStubAiProvider`, a deterministic template renderer used to exercise that contract. It does not call an external LLM or AI service and must not be represented as model-generated analysis.

The Angular Investigation page distinguishes confirmed facts, strong evidence, probable inference, and possible hypotheses; it shows the deterministic timeline and evidence links, and displays AI unavailability without hiding deterministic findings.

## Deterministic DB-pool scenario

Run the generator once after the Compose stack is healthy:

```powershell
docker compose run --rm log-generator --seed 42 --scenario all --count 80 --rate 20
```

Investigating `ORDER_TOKEN=demo-order-42` in the seeded Demo Marketplace PROD scope demonstrates:

1. DB connection pool reaches 50/50 (100%).
2. Settlement PostgreSQL operation times out.
3. Kafka retry and duplicate-delivery signals follow.
4. Kafka consumer lag rises to 125 and is classified as a downstream symptom.

## Security and failure verification

Focused tests cover authorization-before-query, unauthorized PROD and project isolation, bounded evidence packages, allowlisted/scoped PromQL, provider failure, malformed structured output, unsupported citations, and prompt-injection text treated only as evidence data. Runtime verification uses the seeded `support` identity, whose explicit environment grant permits Demo Marketplace PROD; the DEV-only identity receives a non-enumerating 404.
