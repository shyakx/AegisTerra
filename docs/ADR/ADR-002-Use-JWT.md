# ADR-002: Use JWT with Refresh Tokens

## Status

Accepted

## Date

2026-08-05 (expanded from initial baseline)

## Context

AegisTerra serves multiple actor types (administrators, field officers, insurers, finance, oversight) over HTTP APIs and a React SPA. Authentication must be:

- Scalable across web sessions without sticky server session affinity as a hard requirement
- Auditable (login success/failure, refresh, logout)
- Compatible with future API clients and possible IdP federation
- Supportive of **refresh rotation and revocation** for compromised sessions

The architecture review found the current code returns mock token strings while protecting APIs with HTTP Basic — this is **not** an implementation of this ADR.

## Decision

Use **JWT access tokens** (short-lived) plus **opaque or JWT refresh tokens** with:

- Rotation on each refresh
- Server-side revocation / family invalidation on reuse or logout
- RBAC claims (roles/permissions) enforced on the API
- HTTPS-only transport in all non-local environments

Exact browser storage strategy (memory + refresh cookie via BFF vs Authorization Bearer from SPA) must be confirmed in a follow-up security ADR before production; this ADR locks the **token model**, not the browser storage detail.

## Consequences

### Positive

- Stateless access-token validation at API gateway/service layer
- Clear path to multi-client access (SPA, future mobile)
- Revocation/rotation addresses stolen refresh tokens better than infinite JWTs alone

### Negative / costs

- Must implement key management, expiry, and revocation store
- Clock skew and token size must be managed
- Easy to implement incorrectly (as current mock demonstrates)

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Server sessions only (sticky HTTP session) | Harder horizontal scale; awkward for pure API clients |
| HTTP Basic for SPA | Credentials sent every request; poor UX; no fine-grained expiry |
| API keys only | Unsuitable for interactive user RBAC |
| OAuth2/OIDC via external IdP only (day one) | Desired later; local JWT allows progress before IdP procurement |

## Implementation note

**Target:** Phase 2 of `DevelopmentRoadmap.md`. Current mock tokens and HTTP Basic dual-path are technical debt (TD-002) and must be removed for SPA auth.
