# Phase 4 Deliverables — Agricultural Core Domain

**Version:** 0.5.0  
**Status:** Implementation complete — awaiting stakeholder approval before Policy Management  
**Design:** `FarmerDomain.md`

---

## 1. FarmerDomain.md

Authoritative design gate covering vision, aggregates, relationships, business/validation rules, lifecycles, domain events, registration workflow, API surface, and quality gate. Located at `docs/FarmerDomain.md`.

---

## 2. Registration workflow diagram

```text
[1 Household] → [2 Primary Farmer] → [3 Identity Verification]
        → [4 Farm Registration] → [5 Boundary Mapping]
        → [6 Plot Registration] → [7 Crop Registration]
        → [8 Review] → [Submit]

Draft: PUT/POST /api/v1/registration-drafts
Submit: POST /api/v1/registration-drafts/{id}/submit
UI: /farmers/register (MapLibre on step 5)
```

---

## 3. API summary

| Resource | Base path | Notes |
|----------|-----------|-------|
| Households | `/api/v1/households` | search + CRUD |
| Farmers | `/api/v1/farmers` | search, export CSV, CRUD |
| Farms | `/api/v1/farms` | search, export CSV, CRUD |
| Boundaries | `/api/v1/farm-boundaries` | CRUD, `/validate`, `/{id}/activate` |
| Plots | `/api/v1/plots` | list by farm + CRUD |
| Crops | `/api/v1/crops` | reference CRUD |
| Seasons | `/api/v1/seasons` | reference CRUD |
| Crop seasons | `/api/v1/crop-seasons` | history by farm + CRUD |
| Registration | `/api/v1/registration-drafts` | draft + `/submit` |

Swagger UI: `/swagger-ui.html` (authenticated app; docs permitAll).  
RBAC: `farmers:read|write`, `farms:read|write`.

---

## 4. UI summary

| Page | Route |
|------|-------|
| Households | `/households` |
| Farmer list | `/farmers` |
| Farmer profile | `/farmers/:id` |
| Registration wizard | `/farmers/register` |
| Farm list / details | `/farms`, `/farms/:id` |
| Boundary editor | `/farms/:id/boundary` |
| Plots | `/farms/:id/plots` |
| Crop history | `/farms/:id/crop-history` |
| Crops / Seasons | `/crops`, `/seasons` |

Shared list UX: search, filters, sort, pagination, CSV export, loading/error/empty states, responsive layout.

---

## 5. Test summary

| Suite | Coverage |
|-------|----------|
| `GeometryServiceTest` | valid polygon, self-intersection reject, non-polygon reject |
| `AgricultureIntegrationTest` | validate → draft → submit → farmer + ACTIVE boundary |
| `FarmerControllerTest` / `FarmControllerTest` | auth + create against PostGIS |
| Existing Phase 2/3 ITs | still green |

**Result:** `Tests run: 32, Failures: 0, Errors: 0` (Testcontainers PostGIS).

---

## 6. Technical debt (Phase 4-specific)

1. **Overlap detection** — API/TODO hook only; hard ST_Intersects neighbor check deferred.
2. **Admin boundary overlay** — MapLibre editor uses OSM basemap; geography GeoJSON overlay API not exposed yet.
3. **KYC provider** — identity step is operator attestation, not external KYC.
4. **Users page** — still local mock UI (IAM APIs exist; FE not fully wired).
5. **Policy/claim/weather/inspection controllers** — remain mock scaffolds until their phases.
6. **Plot ⊂ farm containment** — warn-level / deferred hard enforce.
7. **Maven wrapper** — not in repo; CI/local need Maven on PATH or Docker Maven image.

---

## Quality gate checklist

- [x] Registration wizard end-to-end (API IT + FE)
- [x] CRUD for agri aggregates
- [x] MapLibre boundary editing + PostGIS store
- [x] Spatial validation (valid, area bounds)
- [x] Search/filter/pagination/export
- [x] Swagger annotations on controllers
- [x] Tests green
- [x] UI responsive; no mock farmer/farm list data
- [x] Audits on create/update/boundary
- [ ] Stakeholder acceptance (required before Policy Management)

**Do not start Policy Management until this phase is accepted.**
