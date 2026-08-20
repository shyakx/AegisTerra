# AegisTerra Data Architecture

**Version:** 3.0.0 (Phase 3 design)  
**Status:** Approved for implementation  
**Depends on:** ADR-001 (PostgreSQL + PostGIS), ADR-003 (Clean Architecture), Phase 2 IAM schema  
**Scope:** Enterprise data platform foundation — **no business workflows** in this phase

This document is the **design gate** for Phase 3. Schema, entities, and repositories must conform to it.

---

## 1. Database Philosophy

1. **Single system of record** — PostgreSQL with PostGIS is the authoritative store for relational, financial, and spatial data.
2. **National scale** — Design for millions of farmers, farms, policies, claims, and observations without redesigning keys or audit patterns.
3. **Domain clarity over convenience** — Bounded contexts own tables; no duplicate “shadow” entities across modules.
4. **Append-friendly audit** — Security and operational mutations are attributable and reconstructable.
5. **Spatial as first-class** — Geometry is not an afterthought column; farms, plots, admin units, and stations have validated geospatial representations.
6. **Evolve via Liquibase** — No ad-hoc DDL in production; every change is versioned, reviewed, and reversible where practical.
7. **Nullable only when business allows absence** — Prefer NOT NULL + defaults; document every nullable column.
8. **Read models later** — OLTP schema first; reporting marts/materialized views are additive, not replacements.

---

## 2. Domain Boundaries (Bounded Contexts)

| Context | Owns | Does not own |
|---------|------|--------------|
| **Identity** (Phase 2) | `users`, `roles`, `permissions`, sessions, tokens, IAM `audit_logs` | Farmers, policies |
| **Party / Organization** | `insurance_companies`, `financial_institutions`, `aggregators`, `partners` | Policies themselves |
| **Geography** | `districts`, `sectors`, `cells`, `villages` (+ optional boundary geometries) | Farm ownership |
| **Agriculture** | `farmers`, `households`, `farms`, `farm_boundaries`, `plots`, `crops`, `seasons`, `crop_seasons` | Claims payout |
| **Insurance** | `policy_types`, `insurance_policies`, `premiums`, `claims`, `claim_assessments`, `payouts` | Weather raw ingest |
| **Climate & Risk** | `weather_stations`, `weather_observations`, `satellite_observations`, `vegetation_indices`, `risk_scores` | Farmer PII |
| **Engagement** | `notifications`, `documents`, `attachments` | Auth sessions |
| **Platform** | `configurations`, platform audit (shared `audit_logs` extended) | Domain rules |

Cross-context references use **UUID foreign keys** only — no shared mutable aggregates across contexts.

---

## 3. Aggregate Roots

| Aggregate root | Contained / strongly consistent |
|----------------|----------------------------------|
| **Farmer** | Links to Household; owns Farm references |
| **Farm** | FarmBoundary(s), Plot(s) |
| **InsurancePolicy** | Premium(s); references Farmer + Farm + PolicyType |
| **Claim** | ClaimAssessment(s); references Policy |
| **Payout** | References Claim (and optionally Premium/Policy) |
| **Season** | CropSeason links (Crop × Season × optional Plot/Farm) |
| **WeatherStation** | WeatherObservation(s) |
| **District** | Sector → Cell → Village hierarchy |

Rules:

- Load/modify one aggregate root per transaction in application services (Phase 4+).
- Phase 3 only provides persistence mapping and repositories — **no use-case orchestration**.

---

## 4. Entity Ownership

| Entity | Owning context | Owning role (business) |
|--------|----------------|------------------------|
| Farmer, Household | Agriculture | Farmer Operations |
| Farm, FarmBoundary, Plot | Agriculture | Field / GIS Operations |
| Crop, Season, CropSeason | Agriculture | Agronomy / Underwriting support |
| PolicyType, InsurancePolicy, Premium | Insurance | Underwriting |
| Claim, ClaimAssessment | Insurance | Claims Operations |
| Payout | Insurance / Finance | Finance |
| InsuranceCompany, FI, Aggregator, Partner | Party | Partnerships / Admin |
| District…Village | Geography | National gazetteer / Admin |
| Weather*, Satellite*, VegetationIndex, RiskScore | Climate & Risk | Risk Intelligence |
| Notification, Document, Attachment | Engagement | Operations |
| Configuration | Platform | System Admin |
| AuditLog | Platform / Security | Auditor / System |

`users` (IAM) may **link** to Farmer or Partner staff via optional `user_id` on Farmer / org membership tables later — not duplicated person records.

---

## 5. Data Lifecycle

Generic status vocabulary (entity-specific enums refine this):

| Phase | Typical statuses |
|-------|------------------|
| Drafting | `DRAFT`, `PENDING_VERIFICATION` |
| Active | `ACTIVE`, `ISSUED`, `PUBLISHED`, `VALIDATED` |
| Terminal soft | `INACTIVE`, `SUSPENDED`, `CANCELLED`, `EXPIRED`, `ARCHIVED` |
| Soft delete | `deleted=true` + often `status=DELETED` |

Lifecycle transitions are **enforced in application/domain layers** (later phases), not in the database alone. DB enforces integrity (FKs, checks, uniques).

---

## 6. Soft Delete Strategy

- Mutable business tables include `deleted BOOLEAN NOT NULL DEFAULT FALSE`.
- Repositories default to `deleted = false` filters.
- Unique constraints that must allow reuse after delete use **partial unique indexes** (`WHERE deleted = false`) on PostgreSQL.
- Soft-deleted rows remain for audit/forensics; hard delete only via controlled purge jobs (ops ADR later).
- IAM `audit_logs` and observation fact tables are **not** soft-deleted in normal flows (append-only).

---

## 7. Archiving Strategy

| Data class | Hot retention | Archive approach |
|------------|---------------|------------------|
| Policies, claims, payouts | Multi-year legal retention | Status `ARCHIVED` + optional history tables; partition by year later |
| Weather / satellite observations | Rolling hot window (e.g. 24–36 months) | Table partition by `observed_at`; detach partitions to cold storage |
| Notifications | Short hot window | Archive or purge acknowledged rows per policy |
| Audit logs | Long retention | Partition by `created_at`; no update/delete from app |

Phase 3: create partition-ready schemas (e.g. `observed_at NOT NULL`, BRIN/btree indexes). Physical partitioning of observation tables may be enabled when volume justifies (document in ops runbook).

---

## 8. Audit Strategy

**Row-level audit columns** on mutable entities:

- `created_at`, `updated_at` (timestamptz, NOT NULL)
- `created_by`, `updated_by` (UUID, nullable only for system/bootstrap)
- `version` (BIGINT, optimistic lock)
- `status` (VARCHAR, NOT NULL)

**Event audit** (`audit_logs` from Phase 2, extended usage):

- Security events already defined
- Phase 4+ domain services append business actions (`FARMER_CREATED`, `POLICY_ISSUED`, …)

**History tables** (Phase 3 creates for high-risk aggregates):

- `insurance_policies_history`
- `claims_history`
- `payouts_history`

History rows are insert-only snapshots on significant status/amount changes (triggers or application writers in later phases; Phase 3 provides table structures).

---

## 9. Versioning Strategy

1. **Optimistic locking** — JPA `@Version` / `version` column on mutable aggregates.
2. **Schema versioning** — Liquibase changesets; never edit applied changesets.
3. **Model versioning** — `risk_scores.model_version`, satellite product versions as columns.
4. **Token version** — already on `users` for auth invalidation (Phase 2).

---

## 10. Naming Conventions

| Object | Convention | Example |
|--------|------------|---------|
| Tables | snake_case plural | `farm_boundaries` |
| Columns | snake_case | `national_id` |
| PK | `id` UUID | |
| FK | `{ref}_id` | `farmer_id` |
| Status | VARCHAR + CHECK or app enum | `ACTIVE` |
| Geometry | `geom` (geography/geometry) | `farm_boundaries.geom` |
| Indexes | `idx_{table}_{cols}` | `idx_farms_farmer_id` |
| Unique | `uk_{table}_{cols}` | `uk_farmers_national_id` |
| History | `{table}_history` | `claims_history` |

API/Java names remain PascalCase / camelCase mapped via JPA.

---

## 11. Partitioning Strategy

**Phase 3 (foundation):**

- Design observation and audit tables with time columns suitable for `RANGE` partitioning.
- Do **not** prematurely partition small reference tables (crops, policy_types, villages).

**Future (when metrics demand):**

- `weather_observations` / `satellite_observations` by month/quarter on `observed_at`
- `audit_logs` by month on `created_at`
- Optional `insurance_policies` by `issued_at` year for very large national volumes

---

## 12. Backup Strategy

| Environment | RPO / RTO targets (initial) | Method |
|-------------|----------------------------|--------|
| Production | RPO ≤ 15 min; RTO ≤ 4 h (tune with ops) | Continuous WAL archiving + daily base backups; tested restore quarterly |
| Staging | Daily | Snapshot / dump |
| Local | Best effort | Volume backup / recreate via Liquibase |

Backups must include PostGIS extension state. Document restore drills in ops runbooks (Phase 10). Encrypted backups for PII databases.

---

## 13. Nullability Policy

**NOT NULL by default** for:

- Identity fields, status, audit timestamps, version, deleted
- Mandatory business keys (national_id when required by law for Farmer)
- Geometry on FarmBoundary when status is `ACTIVE` (DRAFT may allow empty geom — use status-gated app validation)

**Nullable when justified:**

- Optional contact fields (secondary phone)
- `created_by` on system-ingested observations
- Optional FK to User until portal linkage exists
- Soft attributes (notes, external codes)

Every nullable column must appear in migration comments or this doc’s entity section.

---

## 14. Complete Domain Catalog (Phase 3)

### 14.1 Farmer

- **Purpose:** Insured person / claimant identity in agriculture context.  
- **Relationships:** 0..1 Household; 1..* Farms; 1..* Policies; optional `user_id` → IAM User.  
- **Rules:** Unique `national_id` among non-deleted; phone required; location FKs to admin geography preferred.  
- **Lifecycle:** `PENDING_VERIFICATION` → `ACTIVE` → `SUSPENDED`/`INACTIVE`.  
- **Validation:** National ID format (country rules), phone, email optional.  
- **Ownership:** Farmer Operations.  
- **Extensibility:** KYC level, vulnerability flags, preferred language.

### 14.2 Household

- **Purpose:** Grouping of related farmers for subsidy/targeting.  
- **Relationships:** 1..* Farmers.  
- **Lifecycle:** `ACTIVE` / `INACTIVE`.  
- **Ownership:** Farmer Operations.

### 14.3 Farm

- **Purpose:** Productive land unit belonging to a Farmer.  
- **Relationships:** Farmer; 1..* FarmBoundary; 0..* Plot; 0..* Policies.  
- **Rules:** `farm_code` unique per non-deleted; `farm_size_ha` > 0 when ACTIVE.  
- **Lifecycle:** `DRAFT` → `ACTIVE` → `INACTIVE`.  
- **Ownership:** Field Operations.

### 14.4 FarmBoundary

- **Purpose:** Authoritative geometry for a farm (parcel).  
- **Relationships:** Farm.  
- **Rules:** ACTIVE requires valid Polygon/MultiPolygon; SRID 4326 (WGS84) storage; area computable.  
- **Lifecycle:** `DRAFT` → `ACTIVE` → `ARCHIVED`.  
- **Ownership:** GIS Operations.

### 14.5 Plot

- **Purpose:** Subdivision of a farm for crop season assignment.  
- **Relationships:** Farm; optional plot-level geom.  
- **Lifecycle:** `ACTIVE` / `INACTIVE`.  
- **Ownership:** Field Operations.

### 14.6 Crop

- **Purpose:** Reference crop type (maize, rice, …).  
- **Relationships:** CropSeason.  
- **Lifecycle:** `ACTIVE` / `DISABLED`.  
- **Ownership:** Agronomy reference data.

### 14.7 Season

- **Purpose:** Named agricultural season window (e.g. 2026A).  
- **Relationships:** CropSeason; date range.  
- **Lifecycle:** `PLANNED` → `OPEN` → `CLOSED`.  
- **Ownership:** Underwriting / Agronomy.

### 14.8 CropSeason

- **Purpose:** Crop grown in a season on a farm/plot.  
- **Relationships:** Crop, Season, Farm, optional Plot.  
- **Lifecycle:** `PLANNED` → `PLANTED` → `HARVESTED` / `FAILED`.  
- **Ownership:** Field Operations.

### 14.9 InsurancePolicy (table: `insurance_policies`)

- **Purpose:** Contract of cover.  
- **Relationships:** Farmer, Farm, PolicyType; Premiums; Claims.  
- **Rules:** Coverage and premium amounts > 0 when ISSUED/ACTIVE; date consistency.  
- **Lifecycle:** `DRAFT` → `ISSUED` → `ACTIVE` → `CANCELLED`/`EXPIRED`.  
- **Ownership:** Underwriting.  
- **Note:** Named `insurance_policies` to avoid SQL keyword clash with generic “policy”.

### 14.10 PolicyType

- **Purpose:** Product definition (cover rules metadata).  
- **Relationships:** InsurancePolicies.  
- **Lifecycle:** `ACTIVE` / `RETIRED`.

### 14.11 Premium

- **Purpose:** Amount due/paid for a policy period.  
- **Relationships:** InsurancePolicy.  
- **Lifecycle:** `DUE` → `PAID` / `WAIVED` / `OVERDUE`.

### 14.12 Claim

- **Purpose:** Loss notification under a policy.  
- **Relationships:** InsurancePolicy; ClaimAssessments; Payout.  
- **Lifecycle:** `SUBMITTED` → `UNDER_REVIEW` → `APPROVED`/`REJECTED` → `PAID`.  
- **Ownership:** Claims Operations.

### 14.13 ClaimAssessment

- **Purpose:** Inspection/assessment outcome for a claim.  
- **Relationships:** Claim; optional inspector User.  
- **Lifecycle:** `DRAFT` → `SUBMITTED` → `FINAL`.

### 14.14 Payout

- **Purpose:** Financial settlement for an approved claim.  
- **Relationships:** Claim; optional FinancialInstitution.  
- **Lifecycle:** `PENDING` → `APPROVED` → `DISBURSED` / `FAILED`.  
- **Ownership:** Finance.

### 14.15 InsuranceCompany / FinancialInstitution / Aggregator / Partner

- **Purpose:** Organizational parties in the ecosystem.  
- **Relationships:** Policies/payouts/onboarding as FKs.  
- **Lifecycle:** `ACTIVE` / `SUSPENDED` / `INACTIVE`.  
- **Ownership:** Partnerships Admin.

### 14.16 District / Sector / Cell / Village

- **Purpose:** Rwanda-style administrative hierarchy (extensible).  
- **Relationships:** Parent chain; Farmers/Farms reference lowest known level.  
- **Spatial:** Optional `geom` MultiPolygon for boundaries.  
- **Ownership:** Geography reference.

### 14.17 WeatherStation / WeatherObservation

- **Purpose:** Station master + time-series observations.  
- **Spatial:** Station `geom` Point.  
- **Ownership:** Risk Intelligence.

### 14.18 SatelliteObservation / VegetationIndex

- **Purpose:** EO products and derived indices (NDVI, etc.) for farms/regions.  
- **Spatial:** Optional footprint geometry; link `farm_id` when applicable.  
- **Ownership:** Risk Intelligence.

### 14.19 RiskScore

- **Purpose:** Model output for farm/policy/region with confidence + model_version.  
- **Lifecycle:** `DRAFT` → `CALCULATED` → `REVIEWED` → `PUBLISHED` → `DEPRECATED`.

### 14.20 Notification

- **Purpose:** User/org directed messages.  
- **Lifecycle:** `PENDING` → `DELIVERED` → `READ` / `ARCHIVED`.

### 14.21 Document / Attachment

- **Purpose:** Metadata for stored files (KYC, claim evidence); binary in object storage later — Phase 3 stores metadata + `storage_key`.  
- **Relationships:** Polymorphic `owner_type` + `owner_id` **or** dedicated FKs; prefer `owner_type`/`owner_id` with check constraint for extensibility.

### 14.22 AuditLog

- **Purpose:** Append-only security/business event log (Phase 2 table reused).

### 14.23 Configuration

- **Purpose:** Key/value typed system configuration (`key` unique, `value_json`, `category`).  
- **Ownership:** System Admin.

---

## 15. Spatial Standards (summary)

Full detail in `PostGIS.md`.

- **SRID:** 4326 (WGS84) for storage interchange; compute area in geography or projected meters via `geography` type / `ST_Area` on geography.
- **Types:** Point (stations), Polygon/MultiPolygon (farms, admin, footprints).
- **Indexes:** GIST on `geom`.
- **Validation:** `ST_IsValid`, non-empty, reject antimeridian-broken polygons at app layer.
- **API:** GeoJSON in/out at presentation layer (Phase 4+); Phase 3 repositories accept/return JTS or native types.

---

## 16. Performance Principles

- Index all FKs used in joins/filters.
- Composite indexes for common list filters (`farmer_id, status`, `policy_id, status`).
- GIST for spatial; BRIN optional on large time-series later.
- **Offset pagination** for admin UIs; **keyset/cursor** for large feeds (observations, audit).
- Connection pool (HikariCP) sized per env; statement timeouts in prod.
- Caching: reference data (crops, policy_types, admin geography) eligible for read-through cache in later phases — document only in Phase 3 (`RepositoryGuidelines.md`).

---

## 17. Phase 3 Implementation Boundaries

**In scope**

- Liquibase migrations for all catalog tables (+ history tables)
- PostGIS extension
- JPA entity mappings
- Spring Data repositories (interfaces + query methods) — **no services/workflows**
- Docker Compose PostGIS for local
- Testcontainers-based migration/repository/spatial tests
- Docs: DataArchitecture, ERD, PostGIS, RepositoryGuidelines

**Out of scope**

- Farmer/Policy/Claim controllers and UI
- Application use cases / domain services for agriculture & insurance
- File binary storage providers
- Physical partition creation (unless trivial and needed for tests)

---

## 18. Quality Gate Checklist

- [ ] PostgreSQL profile boots against PostGIS
- [ ] Liquibase succeeds on empty database
- [ ] PostGIS enabled; farm boundary geom round-trips
- [ ] Entities + repositories aligned to this catalog
- [ ] Repository/spatial/constraint tests pass
- [ ] ERD reviewed
- [ ] Documentation complete

---

## 19. References

- ADR-001 PostgreSQL + PostGIS  
- `DatabaseDesign.md`  
- `SecurityArchitecture.md` (IAM tables)  
- `DomainModel.md` (superseded/extended by this catalog for Phase 3)  
- `DevelopmentRoadmap.md` Phase 3  

**Next:** `ERD.md`, `PostGIS.md`, `RepositoryGuidelines.md`, then Liquibase + repositories.
