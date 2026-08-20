# ADR-001: Use PostgreSQL with PostGIS

## Status

Accepted

## Date

2026-08-05 (expanded from initial baseline)

## Context

AegisTerra must store and query:

- Relational insurance data (farmers, farms, policies, claims, payouts, users, audit)
- Administrative geography (district → sector → cell → village)
- Farm boundaries and geospatial risk overlays

National agricultural insurance workflows require ACID transactions, strong referential integrity, reporting SQL, and spatial predicates (contains, intersects, area). The platform is expected to run as a long-lived government system, not a disposable prototype.

### Why PostgreSQL (relational core)?

- Mature open-source RDBMS widely supported in government and enterprise hosting
- Excellent SQL ecosystem for audits, reporting, and complex joins across insurance entities
- Strong consistency model appropriate for financial and claims workflows
- Extensible (PostGIS, full-text, JSONB when needed without abandoning relational integrity)

### Why PostGIS (spatial extension)?

- Farm parcels and administrative boundaries are first-class spatial concerns
- Risk, weather, and satellite overlays need spatial joins and indexes
- Avoids bolting a separate spatial database onto the system of record

## Decision

Use **PostgreSQL** as the primary system-of-record database, with the **PostGIS** extension enabled for all geospatial columns, indexes, and queries.

Schema evolution must go through versioned migrations (Liquibase preferred per DatabaseDesign; confirm tooling in implementation phase).

## Consequences

### Positive

- Single datastore for relational + spatial reduces sync complexity
- Spatial indexes support GIS UI and risk analytics
- Familiar operational model for DBAs and government ICT teams

### Negative / costs

- Requires PostGIS-capable hosting and DBA skills
- Spatial migrations need discipline (SRID, indexes, validation)
- Team must avoid storing coordinates as unstructured floats once PostGIS is available

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| NoSQL-only (MongoDB, etc.) | Weak multi-record transactional guarantees for policies/claims; spatial secondary |
| Separate GIS server + non-spatial SQL | Dual writes, consistency risk, higher ops cost |
| MySQL / MariaDB + spatial | Viable but weaker PostGIS ecosystem parity for advanced GIS |
| Cloud proprietary spatial-only store | Vendor lock-in; still need relational SoR for insurance |

## Implementation note

As of architecture review (0.1.0 code), PostgreSQL/PostGIS are **accepted but not yet implemented** in the backend. See `DevelopmentRoadmap.md` Phase 3.
