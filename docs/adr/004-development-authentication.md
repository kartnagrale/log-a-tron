# ADR 004: Profile-isolated development authentication

- Status: Accepted
- Date: 2026-08-26

## Context

Production is an OIDC/JWT resource server, but local development cannot depend on an enterprise identity provider. Disabling authentication would invalidate security testing and risk accidental deployment.

## Decision

The `dev-auth` profile exposes a fixed-identity token endpoint and HMAC JWT encoder/decoder. Tokens have issuer, audience, expiry, and signatures and map to seeded database users. The endpoint and signing beans do not exist outside that profile. Production uses an external issuer and audience.

## Alternatives

Authentication-disabled mode was rejected as unsafe. A bundled full OIDC provider was rejected as unnecessary Phase 1 infrastructure.

## Consequences

Local login is lightweight while exercising the real bearer-token and server-side authorization path. The development secret must be at least 32 bytes and must never be reused outside local development.
