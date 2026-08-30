# ADR 002: Authorization-derived query scope

- Status: Accepted
- Date: 2026-08-26

## Context

Client-controlled project and environment identifiers can cause cross-project exposure if treated as authority.

## Decision

Verified JWT issuer/subject identifies a database user. Active project memberships, roles, and environment/service grants are resolved server-side into immutable `EffectiveScope`. Requested IDs are intersected with that scope. Unauthorized resource-specific lookups return the same 404 contract as missing resources.

## Alternatives

Frontend-only filtering and trusting token role claims were rejected. Returning 403 for existing unauthorized UUIDs was rejected because it enables resource enumeration.

## Consequences

Every future ClickHouse/trace/metric query must accept `EffectiveScope`; it cannot accept raw tenant IDs alone. Collection endpoints omit inaccessible rows, while guessed resource IDs disclose no existence.

