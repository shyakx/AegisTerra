# ADR-003: Adopt Clean Architecture

## Status

Accepted

## Date

2026-08-05 (expanded from initial baseline)

## Context

AegisTerra will grow across domains (IAM, farmers, farms/GIS, policies, claims, finance, weather/risk) and integrations (IdP, payments, satellite). Government systems change slowly in procurement but quickly in policy rules. The codebase must keep **business rules independent** of frameworks (Spring, JPA, HTTP) so that:

- Domain logic is unit-testable without a web container
- Infrastructure (DB, mail, external APIs) can be replaced
- Presentation stays thin

The review found packages named `domain` and `presentation` but **no `application` or `infrastructure`**, with controllers returning mocks and domain types unused. That state does **not** fulfill this ADR.

## Decision

Organize the backend as a **Clean Architecture / hexagonal** modular monolith:

| Layer | Responsibility | Depends on |
|-------|----------------|------------|
| **Domain** | Entities, value objects, domain services, invariants | Nothing outward |
| **Application** | Use cases / ports (in/out), transaction boundaries | Domain |
| **Infrastructure** | JPA, PostGIS, JWT, external clients, mail | Application ports + Domain |
| **Presentation** | Controllers, DTOs, OpenAPI | Application |

Dependency rule: source code dependencies point **inward**. Framework annotations stay at the edges where practical.

## Consequences

### Positive

- Testable core; clearer ownership; safer evolution
- Aligns with long-term maintainability for national systems

### Negative / costs

- More types/files early; discipline required from all contributors
- Risk of “architecture theater” if layers are empty folders

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Classic 3-layer anemic (Controller → Service → Repository) only | Faster short-term; tends to leak rules into services/controllers |
| Microservices from day one | Operational complexity unjustified before domain boundaries stabilize |
| Modular monolith without dependency rules | Modules help, but framework coupling still freezes the core |

## Implementation note

Introduce real `application` and `infrastructure` packages in Phase 1; prove the pattern with Farmer vertical slice in Phase 4 (`DevelopmentRoadmap.md`).
