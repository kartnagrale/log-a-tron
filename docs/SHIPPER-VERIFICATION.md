# Shipper verification — 2026-10-30

The implementation is native to the existing Angular / Spring Boot application. No company server or existing Collector was contacted or changed.

## Local results

- Full backend Maven verify passed locally with 85 tests, zero failures/errors/skips. One additional regression test now pins the existing PLAINTEXT multiline default; the final GitHub workflow runs all 86 tests. Includes PostgreSQL migration/RBAC/concurrent generation/adoption/deployment failure tests, real contrib 0.135.0 ingestion/restart/rollback tests and the existing Kafka-to-ClickHouse pipeline test.
- Frontend production build and 45 tests pass. Existing log, investigation, scope restoration, authentication, capability and theme tests remain included.
- Managed-agent operation fixture passes activation, repeat polling, target validation failure, failed-start restoration and refusal to activate without a rollout identity.
- Browser verification used an isolated local PostgreSQL database/API and synthetic resources. Created project, environment, service, server, instance and two disjoint root/date-folder sources entirely through the UI. Registered a shipper, dismissed its one-time token, generated/previewed/downloaded YAML and confirmed repeated generation retains v1.
- Verified missing-validator feedback, then configured a checksum-verified official Windows contrib 0.135.0 binary and confirmed VALID through the API and displayed UI status.
- Opened and cancelled deployment authorization confirmation. Deployment remains disabled without a real authorized agent; no real deployment was attempted. Deployment/rollback acknowledgements and failure behavior are covered by API/container/shell tests.
- Desktop light/dark and mobile breakpoint screenshots inspected. Mobile document width matches viewport width; YAML and tables scroll within their panels. Browser verification found and fixed icon-derived accessible button names.

## Reproducibility

The Shipper verification GitHub workflow runs the full backend suite with Docker-enabled disposable infrastructure, the agent operation fixture, frontend tests and the production build. The default runtime test image is pinned to contrib 0.135.0; local verification used the repository-built image after checking that exact version.

See SHIPPER-MANAGEMENT.md for required backend destination policy, validator configuration, optional agent setup, legacy cutover, conservative overlap priority and documented source-offset/replay limitations. Ingestion health intentionally remains Unknown until independently verified; APPLIED is a process/health acknowledgement, not an end-to-end ingestion guarantee.
