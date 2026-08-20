# Database Design

## Data Strategy
AegisTerra will use PostgreSQL with PostGIS for geospatial storage and Liquibase for migration management.

**Phase 2 note:** IAM tables are live via Liquibase (`db/changelog`). Local/test profiles currently use H2 (PostgreSQL mode) for developer velocity; **production must use PostgreSQL**. PostGIS spatial objects remain Phase 3.

## Core Design Principles
- UUID primary keys for distributed-safe identity
- Standard audit columns: created_at, updated_at, created_by, updated_by, version, status
- Soft delete support for operational safety and auditability
- Strong integrity constraints and foreign keys
- Spatial indexes for farm boundaries and administrative geography

## Identity entities (implemented Phase 2)
- users
- roles
- permissions
- user_roles
- role_permissions
- login_sessions
- refresh_tokens
- password_history
- password_reset_tokens
- audit_logs
- user_preferences (schema ready)

## Core Entities (later phases)
- farmers / farms / … (Phases 4–5 live — see ERD / DataArchitecture)
- **workflow_*** tables (Phase 6A — see `Database.md`)
- claims / payouts (later consumers)
- satellite_observations
- notifications
- partners
- financial_institutions
- insurance_companies

## Data Governance
- Every mutable entity uses audit metadata
- History tables will be maintained for high-risk entities such as policies, claims, and payouts
- Migrations will be versioned and reversible
