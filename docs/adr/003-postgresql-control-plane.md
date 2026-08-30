# ADR 003: PostgreSQL control plane

- Status: Accepted
- Date: 2026-08-26

## Context

Catalog, grants, configuration, and audit metadata require constraints and transactional updates. Raw logs require a different analytical store later.

## Decision

PostgreSQL owns control-plane metadata. Flyway exclusively creates and evolves the schema; Hibernate validates it. JSONB is limited to flexible labels and non-sensitive audit metadata.

## Alternatives

ClickHouse and document databases were rejected for transactional metadata. PostgreSQL was rejected as future raw-log storage.

## Consequences

Relational constraints protect hierarchy and grants. Phase 2 adds ClickHouse without moving catalog ownership.

