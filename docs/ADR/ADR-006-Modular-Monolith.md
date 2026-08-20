# ADR-006: Prefer a Modular Monolith Deployment

## Status

Accepted

## Date

2026-08-05

## Context

AegisTerra spans multiple domains and future integrations. Premature microservices create network boundaries, distributed transactions, and ops overhead before the domain model is stable. Conversely, a single unstructured codebase becomes a ball of mud.

We need:

- One deployable API especially in early phases
- Clear module boundaries (farmer, farm, policy, claim, iam, risk)
- A path to extract services later **if** scale or team topology demands it

## Decision

Build AegisTerra backend as a **modular monolith**:

- Single Spring Boot deployable
- Package or Maven-module boundaries by bounded context
- Hard rules: no bypassing application layer; no cross-module DB table peeking without published APIs/events
- Asynchronous integration inside the monolith via application services first; messaging introduced with a dedicated ADR when needed

Frontend remains a separate SPA deployable consuming the monolith API.

## Consequences

### Positive

- Simple operations early (one API, one DB)
- Transactional consistency across policy/claim flows
- Faster iteration while discovering domain boundaries

### Negative / costs

- Requires module discipline (linting/archunit recommended later)
- Single deployable can become large — monitor build times and ownership

## Alternatives considered

| Alternative | Why not chosen (now) |
|-------------|----------------------|
| Microservices per domain day one | High ops cost; unclear boundaries; distributed claims/policy consistency pain |
| Distributed modular monolith (many jars, many deploys) | Complexity without benefit pre-PMF |
| Frontend BFF per channel immediately | Add when multiple clients need divergent aggregation |

## Implementation note

Do not split into microservices until Phase 11 drivers are real (scale, independent release cadence, or team Conway needs) and module boundaries are proven.
