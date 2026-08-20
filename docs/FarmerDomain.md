# AegisTerra Farmer Domain (Agricultural Core)

**Version:** 4.0.0 (Phase 4 design)  
**Status:** Approved for implementation  
**Depends on:** Phase 2 IAM, Phase 3 data platform (`DataArchitecture.md`, `PostGIS.md`)  
**Out of scope:** Insurance policies, claims, premiums, payouts, risk scoring workflows

This document is the **design gate** for Phase 4. Services, APIs, and UI must conform to it.

---

## 1. Domain Vision

AegisTerra’s agricultural core is the **digital identity layer for every farmer and every farm** in the national ecosystem. It must:

1. Uniquely identify farmers (national ID, contact, geography).
2. Represent productive land (farms, boundaries, plots) with trustworthy spatial data.
3. Track what is grown where and when (crops, seasons, crop seasons).
4. Support guided onboarding at scale (draft → complete registration).
5. Remain the stable upstream dependency for insurance, weather, AI, claims, and analytics.

Design for **millions of farmers**: pagination/keyset-ready APIs, indexed search, soft delete, auditability, and no mock data in production paths.

---

## 2. Aggregate Roots

| Aggregate | Consistency boundary | Contained / dependent |
|-----------|----------------------|------------------------|
| **Household** | Household membership metadata | Farmers (weak: farmers can exist without household) |
| **Farmer** | Farmer identity & verification | Farms (child aggregate references) |
| **Farm** | Land unit ownership & codes | FarmBoundaries, Plots |
| **FarmBoundary** | Geometry truth for a farm | — (owned by Farm) |
| **Plot** | Subdivision of farm | CropSeason optional link |
| **Crop** | Reference data | — |
| **Season** | Calendar window | — |
| **CropSeason** | Crop×Season×Farm(/Plot) planting | — |
| **RegistrationDraft** | Wizard progress | Serialized step payloads |

**Application rule:** One aggregate root mutated per transaction unless a documented multi-aggregate use case (e.g. registration submit) coordinates several with clear ordering.

---

## 3. Entity Relationships

```text
Household 1──* Farmer
Farmer 1──* Farm
Farm 1──* FarmBoundary
Farm 1──* Plot
Crop 1──* CropSeason
Season 1──* CropSeason
Farm 1──* CropSeason
Plot 0..1──* CropSeason
District/Sector/Cell/Village ← Farmer, Farm (FKs)
Aggregator/Partner ← Farmer (optional)
User ← Farmer.user_id (optional portal link)
RegistrationDraft → farmer_id? / created_by user
```

See also `ERD.md` (agriculture section).

---

## 4. Business Rules

### Household
- Code unique among non-deleted households.
- Soft-deleting a household does not cascade-delete farmers; farmers keep `household_id` or nullify via explicit unlink.

### Farmer
- `national_id` unique among non-deleted.
- `phone_number` unique among non-deleted (enterprise contact channel).
- `email` unique among non-deleted when present.
- Cannot activate (`ACTIVE`) without: national_id, phone, first/last name, and at least one farm with an `ACTIVE` boundary (registration complete rule).
- Suspend/deactivate preserve history; soft delete hides from default lists.

### Farm
- `farm_code` unique globally among non-deleted (system-generated or operator-supplied).
- Farm name unique **within the same farmer** (not global) among non-deleted.
- `ACTIVE` requires `farm_size_ha > 0` OR derived area from ACTIVE boundary ≥ minimum.
- Belong to exactly one farmer.

### FarmBoundary
- Stored as `MultiPolygon` SRID 4326.
- `DRAFT` may omit geom; `ACTIVE` requires valid, non-empty, non-self-intersecting geometry.
- Area (ha) computed via `ST_Area(geom::geography)/10000`.
- Min area default **0.01 ha**; max area default **500 ha** (configurable via `configurations`).
- One preferred ACTIVE boundary per farm (activating a new one archives previous ACTIVE).

### Plot
- `plot_code` unique per farm among non-deleted.
- Optional geom; if present, must be valid and preferably within farm boundary (warn now; hard enforce later).

### Crop / Season
- Reference codes unique among non-deleted.
- Season `end_date >= start_date`.

### CropSeason
- Unique (farm_id, season_id, crop_id, plot_id) among non-deleted (`plot_id` null treated as distinct bucket).
- `planted_area_ha` ≤ farm/plot area when those are known (soft warn → hard fail on ACTIVE plant).

### Registration
- Drafts owned by creating user; expire after configurable days (default 30).
- Submit is atomic: validates all steps, creates/updates aggregates, emits events, audits.

---

## 5. Validation Rules (API / service)

| Field | Rule |
|-------|------|
| National ID | Required; length/format configurable (Rwanda 16-digit default regex); unique |
| Phone | Required; E.164-ish / local `07########` accepted; unique |
| Email | Optional; RFC-lite; unique if set |
| Names | Required; 1–100 chars |
| Farm name | Required; unique per farmer |
| Geometry | `ST_IsValid`; not empty; no self-intersection; area bounds |
| GeoJSON | Polygon or MultiPolygon only; coordinates lon/lat |

Duplicate farm-boundary **overlap detection** is future-ready (API hook / flagged TODO); Phase 4 stores geometry and validates validity/area only.

---

## 6. Lifecycles & State Transitions

### Farmer
```
PENDING_VERIFICATION → ACTIVE
PENDING_VERIFICATION → INACTIVE
ACTIVE → SUSPENDED → ACTIVE
ACTIVE → INACTIVE
* → DELETED (soft)
```

### Farm
```
DRAFT → ACTIVE
ACTIVE → INACTIVE
DRAFT → INACTIVE
* → DELETED (soft)
```

### FarmBoundary
```
DRAFT → ACTIVE  (requires valid geom)
ACTIVE → ARCHIVED
DRAFT → ARCHIVED
```

### Plot
```
ACTIVE ↔ INACTIVE
```

### Crop
```
ACTIVE ↔ DISABLED
```

### Season
```
PLANNED → OPEN → CLOSED
```

### CropSeason
```
PLANNED → PLANTED → HARVESTED
PLANTED → FAILED
```

### RegistrationDraft
```
IN_PROGRESS → SUBMITTED
IN_PROGRESS → ABANDONED / EXPIRED
```

Illegal transitions throw domain/application errors (HTTP 409/422).

---

## 7. Domain Events

Emitted after successful commits (persist to `audit_logs` and optional in-process listeners):

| Event | Payload highlights |
|-------|-------------------|
| `HouseholdCreated` / `Updated` | id, code |
| `FarmerCreated` / `Updated` / `StatusChanged` | id, nationalId, status |
| `FarmCreated` / `Updated` / `StatusChanged` | id, farmerId, farmCode |
| `FarmBoundaryCreated` / `Updated` / `Activated` | id, farmId, areaHa |
| `PlotCreated` / `Updated` / `Deleted` | id, farmId |
| `CropSeasonCreated` / `Updated` | id, farmId, cropId, seasonId |
| `RegistrationDraftSaved` | draftId, step |
| `RegistrationSubmitted` | draftId, farmerId |

Audit store includes: actor user id, timestamp, action, resource, correlation id, **old/new JSON**, optional reason.

---

## 8. Registration Workflow

```text
[1 Household] → [2 Primary Farmer] → [3 Identity Verification]
        → [4 Farm Registration] → [5 Boundary Mapping]
        → [6 Plot Registration] → [7 Crop Registration]
        → [8 Review] → [Submit]
```

| Step | Data captured | Can skip? |
|------|---------------|-----------|
| 1 Household | code, head_name (create or select existing) | Yes (single-farmer) |
| 2 Farmer | names, contacts, admin geography | No |
| 3 Identity | national_id confirm; mark verification pending/passed | No |
| 4 Farm | name, code (optional auto), size estimate | No |
| 5 Boundary | GeoJSON polygon via MapLibre draw | No for submit |
| 6 Plots | zero or more plots | Yes |
| 7 Crops | crop + season links | Yes (can add later) |
| 8 Review | read-only summary | No |

**Draft saving:** After any step, `PUT /api/v1/registration-drafts/{id}` persists JSON blob + `current_step`.  
**Submit:** `POST /api/v1/registration-drafts/{id}/submit` runs validations and creates ACTIVE/PENDING entities per rules.

---

## 9. Search & Listing

Enterprise search endpoints support:

- National ID, phone, farmer name, farm name, farm code  
- Household code  
- Village / cell / sector / district  
- Crop, season  
- Optional bbox / near-point (GPS) for farms with boundaries  

All list APIs: **pagination** (`page`, `size`), **sort**, **filters**, CSV export where listed in API summary.

Permissions: `farmers:read` / `farmers:write`, `farms:read` / `farms:write` (Phase 2 seeds).

---

## 10. API Surface (canonical)

| Method | Path | Purpose |
|--------|------|---------|
| CRUD | `/api/v1/households` | Household management |
| CRUD + search | `/api/v1/farmers` | Farmer management |
| GET | `/api/v1/farmers/{id}` | Profile |
| CRUD | `/api/v1/farms` | Farms |
| CRUD | `/api/v1/farm-boundaries` | Boundaries (+ validate/area) |
| POST | `/api/v1/farm-boundaries/validate` | Geometry validation only |
| CRUD | `/api/v1/plots` | Plots |
| CRUD | `/api/v1/crops` | Crop reference |
| CRUD | `/api/v1/seasons` | Seasons |
| CRUD | `/api/v1/crop-seasons` | Plantings / history |
| CRUD | `/api/v1/registration-drafts` | Wizard drafts |
| POST | `/api/v1/registration-drafts/{id}/submit` | Complete registration |
| GET | `/api/v1/farmers/export` | CSV |
| GET | `/api/v1/farms/export` | CSV |

OpenAPI annotations required on all controllers.

---

## 11. Frontend IA

| Page | Route |
|------|-------|
| Households | `/households` |
| Farmer list | `/farmers` |
| Farmer profile | `/farmers/:id` |
| Registration wizard | `/farmers/register` |
| Farm details | `/farms/:id` |
| Boundary editor | `/farms/:id/boundary` |
| Plot management | `/farms/:id/plots` |
| Crops | `/crops` |
| Seasons | `/seasons` |
| Crop history | `/farms/:id/crop-history` |

UX requirements: search, filters, sort, pagination, CSV export, loading/empty/error states, responsive, accessible labels, **no mock seed data**.

MapLibre: draw / edit / delete polygon; show area; block invalid/self-intersecting; optional admin boundary overlay (GeoJSON from geography API when available).

---

## 12. Application Services

| Service | Responsibility |
|---------|----------------|
| `HouseholdService` | CRUD, uniqueness |
| `FarmerService` | CRUD, search, status transitions, uniqueness |
| `FarmService` | CRUD, per-farmer name uniqueness, status |
| `BoundaryService` | GeoJSON↔JTS, validate, area, activate/archive |
| `PlotService` | CRUD under farm |
| `CropService` / `SeasonService` | Reference CRUD |
| `CropSeasonService` | Planting records / history |
| `RegistrationService` | Draft lifecycle + submit orchestration |
| `AgricultureAuditService` | Structured audit writes |

Controllers: thin — authz, DTO validation, call services.

---

## 13. Schema additions (Phase 4)

Liquibase changeset(s) as needed:

- `farmers.farmer_code` (unique, system-friendly search key) if not present  
- `registration_drafts` table  
- Optional uniqueness indexes for phone/email  
- Config keys for min/max boundary area  

Reuse Phase 3 tables otherwise.

---

## 14. Future Extensibility

- KYC provider integration on identity step  
- Hard spatial containment plot⊂farm and farm-overlap detection  
- Mobile offline draft sync  
- Household multi-farmer roles (head, member)  
- Link Farmer ↔ User for self-service portal  

---

## 15. Quality Gate

- [x] Registration wizard end-to-end  
- [x] CRUD for all agri aggregates  
- [x] MapLibre boundary editing + PostGIS store  
- [x] Spatial validation (valid, area bounds)  
- [x] Search/filter/pagination/export  
- [x] Swagger complete  
- [x] Tests green (unit + IT + controller + geometry)  
- [x] UI responsive; no mock list data  
- [x] Audits verified for create/update/boundary  

**Implementation status:** Complete in **0.5.0** — see `Phase4Deliverables.md`.  
**Do not start Policy Management until Phase 4 is accepted.**
