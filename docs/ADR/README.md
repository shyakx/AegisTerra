# Architecture Decision Records (ADR) Index

This folder records **accepted architectural decisions** for AegisTerra.

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](./ADR-001-Choose-PostGIS.md) | PostgreSQL with PostGIS | Accepted |
| [ADR-002](./ADR-002-Use-JWT.md) | JWT access + refresh (rotation/revocation) | Accepted |
| [ADR-003](./ADR-003-Clean-Architecture.md) | Clean Architecture layering | Accepted |
| [ADR-004](./ADR-004-Choose-React.md) | React + TypeScript SPA | Accepted |
| [ADR-005](./ADR-005-Choose-Spring-Boot.md) | Spring Boot backend | Accepted |
| [ADR-006](./ADR-006-Modular-Monolith.md) | Modular monolith (not microservices-first) | Accepted |
| [ADR-007](./ADR-007-HttpOnly-Cookie-Tokens.md) | HttpOnly cookie token transport for SPA | Accepted |
| [ADR-008](./ADR-008-Reusable-Workflow-Engine.md) | Reusable in-process workflow engine | Accepted |
| [ADR-009](./ADR-009-Administrative-Geography-vs-Agroecological-Classification.md) | Administrative geography vs agroecological classification | Accepted |
| [ADR-010](./ADR-010-Product-Focus-Climate-Yield-Intelligence.md) | Product focus: climate & yield intelligence first | Accepted |
| [ADR-011](./ADR-011-Maize-Yield-ML-Spike.md) | Experimental maize yield ML spike (Stage A) | Accepted (experimental) |

## Format

Each ADR includes: Context, Decision, Consequences, Alternatives considered.

## Rule

Code that contradicts an accepted ADR is **technical debt**, not a silent new decision. Superseding an ADR requires a new ADR with status Superseded on the old one.

## Likely next ADRs

- Liquibase confirmed as migration tool (in use as of Phase 2)
- Multi-tenancy isolation model
- External IdP (OAuth2/OIDC)
- Messaging / transactional outbox for integrations
- BPMN runtime adapter (if/when external engine is adopted)
