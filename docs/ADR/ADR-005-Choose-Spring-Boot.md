# ADR-005: Use Spring Boot for the Backend Platform

## Status

Accepted

## Date

2026-08-05

## Context

The backend must expose versioned REST APIs, integrate security, persistence, validation, observability, and eventually messaging/integrations. Java remains a common, supportable choice for government enterprise systems. The repository already uses Spring Boot 3.3.x.

Needed capabilities:

- Security filter chains and method security
- Validation, Actuator, OpenAPI
- JPA/JDBC + transaction management
- Test slicing and broad hiring/support ecosystem

## Decision

Use **Java** with **Spring Boot 3.x** as the backend application framework, structured per ADR-003 (Clean Architecture) inside a modular monolith (ADR-006).

Toolchain version must be singular: align README and `pom.xml` (prefer **Java 21** LTS if runtime allows; otherwise document Java 17 explicitly).

## Consequences

### Positive

- Batteries-included enterprise features
- Strong fit with PostgreSQL/PostGIS via Spring Data
- Mature testing and security libraries

### Negative / costs

- Framework gravity can violate Clean Architecture if controllers grow fat
- Upgrade cadence must be planned for security patches

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Quarkus / Micronaut | Strong options; Spring already chosen and familiar |
| .NET | Viable; team/repo already on JVM |
| Node/NestJS monolith | Weaker default fit for strict transactional insurance domains in this org context |
| Python (Django/FastAPI) | Good for ML sidecars later; not primary SoR API here |

## Implementation note

Keep Spring at the edges (presentation/infrastructure). Domain modules should remain framework-light.
