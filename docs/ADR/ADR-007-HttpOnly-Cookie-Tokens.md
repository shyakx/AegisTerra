# ADR-007: HttpOnly Cookie Token Transport for SPA

## Status

Accepted

## Date

2026-08-05

## Context

ADR-002 requires JWT access tokens and refresh tokens with rotation. Phase 1 stored tokens in `localStorage`, which is readable by any XSS script. AegisTerra is a government-facing SPA that will handle PII and financial workflows.

## Decision

Deliver access and refresh tokens to the browser as **HttpOnly, Secure (prod), SameSite** cookies. Do not store access or refresh tokens in `localStorage` or `sessionStorage`. Allow `Authorization: Bearer` for non-browser clients and automated tests.

Refresh tokens remain opaque server-side hashed values; access tokens remain JWTs.

## Consequences

- Frontend must use `credentials: 'include'` and same-origin or explicit CORS with credentials.
- CSRF readiness is required (SameSite + optional CSRF header strategy per SecurityArchitecture.md).
- XSS cannot read token values via JavaScript.

## Alternatives considered

| Alternative | Why not |
|-------------|---------|
| localStorage Bearer only | XSS exfiltration risk |
| Memory-only access + cookie refresh | Good hardening; more complex SPA; cookies for both chosen for operational simplicity |
| Server sessions only | Rejects ADR-002 scalability goals |
