# ADR 001: Modular monolith control plane

- Status: Accepted
- Date: 2026-08-26

## Context

The control plane needs catalog, authorization, administration, and audit behavior, but does not yet have independently measured scaling boundaries for each domain.

## Decision

Deploy these logical modules together in `platform-api`. Keep `log-processor` as a separate Phase 2 Maven/deployment boundary because ingestion will scale and fail independently.

## Alternatives

Independent microservices were rejected as premature operational and consistency cost. A single unstructured package was rejected because it would erode security and ownership boundaries.

## Consequences

Phase 1 has one transactional control-plane deployment. Package and ArchUnit rules preserve future extraction options without distributed-system overhead.

