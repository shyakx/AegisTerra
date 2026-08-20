# AegisTerra Climate Data Platform

**Version:** 1.0.1 (Climate Data Platform design gate)  
**Status:** **Implemented** in **0.14.0** (shipped with Phase 8B)  
**Target release (implementation):** **0.14.0** (Phase 8A+8B combined release; design preview cited 0.13.0 for 8A alone)  
**Depends on:** Phase 2 IAM · Phase 3 PostGIS · Phase 4 Agriculture (optional farm spatial context) · ArchitectureGovernance  
**Related:** `ArchitectureGovernance.md`, `DevelopmentRoadmap.md`, `ClimateIntelligencePlatform.md`, Liquibase `008-climate-schema.yaml` (foundational placeholders only)  

> **Scope boundary:** This platform collects, normalizes, validates, stores, and exposes climate data.  
> **Out of scope:** AI / ML, Claims logic, Insurance pricing logic, Risk scoring, parametric indices, vegetation analytics products. Those belong to Climate Intelligence / Risk (**Phase 8B** — see `ClimateIntelligencePlatform.md`).

---

## 1. Domain Vision

The Climate Data Platform is the **system of record for environmental observations** in AegisTerra. It is a reusable enterprise data platform — not a weather widget, not a claims helper, and not a risk engine.

It must:

1. Ingest climate facts from heterogeneous **providers** through a stable SPI.
2. **Normalize** units, timestamps (UTC), CRS (EPSG:4326), and variable codes into a canonical model.
3. **Validate** and score **data quality** before observations become queryable as “trusted.”
4. Persist **high-volume time-series** and **spatial** references using PostGIS and partition-friendly schemas.
5. Expose read APIs and operator UIs for stations, observations, datasets, imports, and maps.
6. Publish domain events so later Risk / Claims / Insurance modules can subscribe — without coupling into those domains.

**Non-goals (Phase 8A):**

- RiskScore calculation, drought indices, NDVI products as business outputs.
- Calling climate APIs from Claims, Insurance, or Settlement services.
- Training or hosting AI models.
- Real-time alerting engines with SLA (may consume climate data later).
- Implementing National Met / OpenWeather / NASA / Copernicus / etc. adapters (SPI stubs only).

**Honest baseline:** Liquibase `008` already created thin `weather_stations`, `weather_observations`, `satellite_observations` (plus `vegetation_indices` / `risk_scores` which are **not** Climate Data Platform owned). Phase 8A design evolves climate tables into a governed platform; Risk/VI tables stay outside this bounded context.

---

## 2. Business Goals

| Goal | Outcome |
|------|---------|
| Single climate truth | One place for stations, observations, EO products, and import lineage |
| Provider independence | Swap or add providers without changing consumers |
| Auditability | Every batch/import has job, provider, validation, and quality records |
| Scale | Design for millions–billions of observation rows over national coverage |
| Safe reuse | Farms/policies/claims reference climate by ID/time/bbox — never raw provider payloads |
| Operator control | Manual import, revalidation, quarantine, and retention policies |

---

## 3. Bounded Context

```text
┌─────────────────────────────────────────────────────────────┐
│                  Climate Data Platform                      │
│  Providers · Import Jobs · Stations · Observations ·        │
│  Satellite Obs · Raster Layers · Datasets · Validation ·    │
│  Quality · Cache                                            │
└───────────────┬───────────────────────────┬─────────────────┘
                │ events / read APIs        │ read APIs
                ▼                           ▼
     Climate Intelligence (8B+)      Agriculture / GIS views
     Risk / Index / Alerts           (farm geometry context)
                │
                ▼
         Insurance · Claims (consumers only — later)
```

| May depend on | Must not depend on |
|---------------|--------------------|
| Identity (RBAC, audit) | Claims persistence / payment |
| PostGIS / Geography | Insurance pricing / underwriting services |
| EventBus (publish) | Settlement / ledger |
| Object storage SPI (raster blobs) | Workflow for climate ingest (optional later; v1 = jobs) |
| Agriculture `farm_id` as optional FK/ref | Modifying Workflow/Task/Decision engines |

**Package plan (implementation later):**

- `application/climate/*` — ingest, validation, quality, dataset, reporting, cache
- `application/climate/spi/*` — `ClimateDataProvider` adapters (stubs)
- `domain/climate/*` — statuses, variable codes, quality enums
- `infrastructure/persistence/climate/*` — entities/repos
- `presentation/*Climate*Controller`

---

## 4. Supported Climate Sources

Extension points (config + SPI). **Do not implement integrations in 8A design approval.**

| Provider code | Category | Typical products |
|---------------|----------|------------------|
| `NATIONAL_MET` | In-situ / official | Station observations, bulletins |
| `OPENWEATHER` | Commercial forecast/obs | Current, historical, grids |
| `TOMORROW_IO` | Commercial | Timeline, maps, alerts feeds |
| `NASA` | EO / science | POWER, MODIS, etc. |
| `COPERNICUS` | EO | Climate Data Store, Sentinel Hub |
| `SENTINEL` | EO | Sentinel-1/2 scenes, STAC |
| `PLANET` | Commercial EO | PlanetScope scenes |
| `NOAA` | Met / climate | GHCN, GOES, grids |
| `OTHER` / custom | Extensible | Partner CSV, MoUs |

Each provider registers as `ClimateProvider` with: code, display name, auth config (secrets vault ref), enabled flag, capabilities (`STATION_OBS`, `GRID`, `SATELLITE`, `RASTER`), rate limits, and timezone defaults.

---

## 5. Weather Data Model

### Weather Station

| Attribute | Notes |
|-----------|-------|
| `id`, `code`, `name` | Business code unique when active |
| `geom` Point 4326 | Required |
| `elevation_m` | Optional |
| `provider_code`, `external_station_id` | Lineage |
| `district_code` / admin refs | Optional denormalized |
| `status`, audit, optimistic lock | Soft-delete allowed |

### Weather Observation (append-only fact)

| Attribute | Notes |
|-----------|--------|
| `id` | UUID |
| `station_id` | FK |
| `observed_at` | Timestamptz UTC |
| `variable_code` | Canonical: `TEMP_C`, `RAIN_MM`, `HUMIDITY_PCT`, `WIND_MS`, `PRESSURE_HPA`, … |
| `value` | Numeric |
| `unit` | Canonical unit after normalize |
| `quality_flag` | `RAW` / `VALID` / `SUSPECT` / `REJECTED` / `QUARANTINED` |
| `provider_code`, `import_job_id` | Lineage |
| `source_payload_hash` | Dedup |
| **No soft-delete** | Corrections = new observation or adjustment row |

**Evolution from 008:** Prefer **long-format** (variable_code + value) over wide columns (`temperature_c`, `rainfall_mm`, …) for extensibility; migration strategy in implementation phase may dual-write then cut over.

---

## 6. Satellite Data Model

### Satellite Observation

| Attribute | Notes |
|-----------|--------|
| `id`, `product_id`, `product_version` | Scene/product identity |
| `provider_code`, `external_id` | Lineage |
| `observed_at` / `acquired_at` | Sensing time |
| `cloud_cover_pct` | Optional |
| `footprint` MultiPolygon 4326 | Spatial |
| `bbox` | Optional envelope for fast filter |
| `storage_uri` | Object store / STAC asset URI (not DB blob) |
| `farm_id` | **Optional** weak link for farm-scoped products only |
| `dataset_id` | Optional membership |
| `quality_flag`, `import_job_id` | Governance |

**Not in Climate Data Platform:** derived VI products (`vegetation_indices`) — those are Intelligence/Risk consumers of satellite observations.

### Raster Layer

| Attribute | Notes |
|-----------|--------|
| `id`, `code`, `name` | Catalog entry |
| `provider_code`, `variable_code` | What the grid represents |
| `crs`, `resolution_m`, `bbox` | Spatial metadata |
| `observed_at` or valid time range | Temporal |
| `storage_uri`, `format` | GeoTIFF/COG/NetCDF pointer |
| `checksum`, `byte_size` | Integrity |

Rasters are **references + metadata** in Postgres; pixels live in object storage or PostGIS raster (future opt-in). Prefer COG + URI for v1.

---

## 7. Observation Model

Unified conceptual model:

```text
ClimateObservation (concept)
  ├── WeatherObservation   (point / station-linked)
  ├── SatelliteObservation (scene / footprint)
  └── RasterLayer          (grid / bbox + URI)
```

Shared cross-cutting:

- `ClimateProvider`
- `ClimateImportJob`
- `ClimateDataset` (logical collection / campaign / season pack)
- `ValidationResult` (per row or per batch)
- `QualityReport` (batch or dataset rollup)

**Climate Dataset**

| Attribute | Notes |
|-----------|--------|
| `id`, `code`, `name`, `description` | Catalog |
| `dataset_type` | `STATION_SERIES`, `SATELLITE_COLLECTION`, `RASTER_STACK`, `MIXED` |
| `time_start`, `time_end` | Coverage |
| `bbox` / footprint | Spatial coverage |
| `provider_code` | Primary source |
| `status` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |

**Climate Import Job**

| Attribute | Notes |
|-----------|--------|
| `id`, `job_number` | e.g. `CLIM-IMP-2026-000000001` |
| `provider_code`, `job_type` | `API_PULL`, `FILE_UPLOAD`, `STAC_HARVEST`, `MANUAL` |
| `status` | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `PARTIAL` |
| `requested_window`, `params_json` | Request |
| `rows_read`, `rows_accepted`, `rows_rejected` | Counts |
| `started_at`, `completed_at`, `error_summary` | Ops |

**Validation Result**

| Attribute | Notes |
|-----------|--------|
| `id`, `import_job_id` | Parent |
| `subject_type`, `subject_id` | Observation or station |
| `rule_code`, `severity` | `ERROR` / `WARN` |
| `message`, `occurred_at` | Detail |

**Quality Report**

| Attribute | Notes |
|-----------|--------|
| `id`, `scope_type` | `JOB` / `DATASET` / `STATION` / `PROVIDER` |
| `scope_id`, `period_start`, `period_end` | Window |
| `metrics_json` | Completeness, outlier rate, latency, duplicate rate |
| `overall_grade` | `A`–`F` or numeric |
| `generated_at` | |

---

## 8. Time-Series Strategy

Target: **millions to hundreds of millions** of weather observations; satellite rows fewer but larger payloads off-DB.

| Concern | Design |
|---------|--------|
| Write path | Append-only; batch insert; idempotent on `(provider, external_id)` or `(station_id, observed_at, variable_code, source_payload_hash)` |
| Partitioning | Range partition `weather_observations` by `observed_at` (monthly or quarterly); satellite by month |
| Indexes | `(station_id, observed_at)`, `(observed_at)`, `(provider_code, observed_at)`, partial on `quality_flag = 'VALID'` |
| Hot path | Recent N days in primary; older partitions may move to cheaper tablespace later |
| Aggregation | Materialized daily/hourly rollups (`climate_observation_aggregates`) refreshed by job — **not** calculated in Claims |
| Retention | See §13; detach/drop old partitions after archive |
| Query API | Require time window + station/bbox; reject unbounded scans |

**Aggregation policy (platform-owned):**

- Hourly / daily means, mins, maxes, rainfall sums per station/variable.
- Aggregates are derived facts with `source = AGGREGATOR` and lineage to job id.
- Consumers (Risk later) read aggregates; they do not re-scan raw forever.

---

## 9. Spatial Data Strategy

| Feature | Approach |
|---------|----------|
| CRS | **EPSG:4326** canonical; reject other CRS at ingest unless transformed |
| Points | Station `geom`; observation may inherit station location |
| Polygons / MultiPolygons | Satellite footprints; dataset coverage |
| Bounding boxes | Stored as `geometry` envelope or `numrange` + xmin/xmax for filters |
| Rasters | URI + bbox + CRS + resolution; optional PostGIS raster later |
| Validation | Lon ∈ [-180,180], lat ∈ [-90,90]; WKT/GeoJSON parse; non-empty; area caps for footprints |
| Indexes | GiST on station geom, satellite footprint, dataset bbox |
| Farm linkage | Optional `farm_id`; spatial join to farms for map UI — no Claims coupling |

Align with existing PostGIS extension (`003`) and farm boundaries from Agriculture.

---

## 10. External Provider SPI

```text
ClimateDataProvider
  code()
  capabilities()
  validateConfig()
  fetchStationObservations(request) → ProviderBatch
  fetchSatelliteProducts(request) → ProviderBatch
  fetchRasterLayers(request) → ProviderBatch
  listStations(request) → ProviderBatch
  healthCheck()
```

| Implementation | Phase 8A |
|----------------|----------|
| `ManualClimateProvider` / file CSV upload | **Design for implement** |
| National Met, OpenWeather, Tomorrow.io, NASA, Copernicus, Sentinel, Planet, NOAA | **Stub only** |

Rules:

- Adapters return **provider-native DTOs**; application layer maps to canonical model.
- Secrets never in DB plaintext — vault/env refs in `config_json`.
- Rate limiting and backoff at adapter boundary.
- Never call SPI from Claims/Insurance controllers.

---

## 11. Data Validation Rules

| Rule code | Severity | Description |
|-----------|----------|-------------|
| `OBS_TIME_REQUIRED` | ERROR | `observed_at` present and not future > skew |
| `OBS_TIME_SKEW` | WARN/ERROR | Clock skew vs ingest time (configurable) |
| `UNIT_KNOWN` | ERROR | Unit maps to canonical |
| `VALUE_RANGE_*` | ERROR | Physical bounds (e.g. humidity 0–100, temp −90–60 °C) |
| `STATION_EXISTS` | ERROR | Station active |
| `CRS_4326` | ERROR | Geometry CRS |
| `COORD_BOUNDS` | ERROR | Lat/lon bounds |
| `FOOTPRINT_SIMPLE` | WARN | Excessive vertices / invalid topology |
| `DEDUP_HASH` | ERROR | Duplicate of accepted observation |
| `CLOUD_COVER_RANGE` | WARN | 0–100 |
| `PROVIDER_ENABLED` | ERROR | Provider must be enabled |

Pipeline: **Ingest → Normalize → Validate → Quality score → Persist (VALID/SUSPECT/REJECTED)**.

Rejected rows remain in quarantine / rejection log linked to `ValidationResult`; they do not enter trusted query defaults.

---

## 12. Data Quality Rules

Quality is separate from schema validation:

| Metric | Intent |
|--------|--------|
| Completeness | Expected cadence vs actual counts per station/day |
| Timeliness | Lag from `observed_at` to ingest |
| Consistency | Cross-variable checks (e.g. humidity vs rain) — WARN only in 8A |
| Outlier rate | Spike detection vs rolling median (rules engine, not ML) |
| Duplicate rate | Dedup hits per job |
| Spatial coverage | % of target bbox with data |

`QualityReport` grades drive operator dashboards. Default API filters: `quality_flag IN (VALID, SUSPECT)` with `VALID` preferred for external consumers.

---

## 13. Historical Data Retention

| Tier | Window (suggested defaults) | Storage |
|------|-----------------------------|---------|
| Hot | 0–24 months | Primary partitioned tables |
| Warm | 2–7 years | Same DB, colder tablespace / compressed partitions |
| Cold | 7+ years | Object archive (Parquet/CSV dumps) + catalog pointer |
| Legal hold | As required | Exempt from drop |

Policies:

- Configured per `variable_code` / dataset (rainfall may retain longer).
- Partition `DETACH` + archive job; never silent delete of trusted facts.
- Soft-delete applies to stations/providers/datasets catalog — **not** observation facts.

---

## 14. Caching Strategy

| Layer | What | TTL / invalidation |
|-------|------|--------------------|
| API response cache | Station metadata, provider list | Minutes; invalidate on write |
| Aggregate cache | Daily rollups for dashboard | Hours; refresh after aggregate job |
| Tile / map cache | Station clusters, footprints simplified | Short TTL; bbox keyed |
| Provider response cache | Raw API pulls during import | Job-scoped; not long-lived secrets |
| CDN (future) | Raster previews | Asset versioned URIs |

Do **not** cache unbounded raw observation dumps. Prefer aggregates for UI charts.

---

## 15. REST API Plan

**Design only — no implementation in this gate.**

Base: `/api/v1`  
Permissions (planned): `climate:read`, `climate:write`, `climate:import`, `climate:admin`, `reports:climate`

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/climate/providers` | List providers |
| GET | `/climate/stations` | Search stations (bbox, q, status) |
| GET | `/climate/stations/{id}` | Station detail |
| POST | `/climate/stations` | Register station (`climate:write`) |
| GET | `/climate/observations` | Time-series query (required from/to) |
| GET | `/climate/observations/search` | POST body search |
| GET | `/climate/satellite-observations` | EO search |
| GET | `/climate/satellite-observations/{id}` | Detail |
| GET | `/climate/raster-layers` | Raster catalog |
| GET | `/climate/datasets` | Dataset browser |
| GET | `/climate/datasets/{id}` | Dataset detail |
| GET/POST | `/climate/import-jobs` | List / start import |
| GET | `/climate/import-jobs/{id}` | Job status + counts |
| GET | `/climate/import-jobs/{id}/validations` | Validation results |
| GET | `/climate/quality-reports` | Quality rollups |
| GET | `/climate/reports/{name}` | Named reports |
| GET | `/climate/map/stations` | GeoJSON for map |
| GET | `/climate/map/footprints` | GeoJSON footprints |

OpenAPI tags: Climate Providers, Stations, Observations, Satellite, Rasters, Datasets, Import Jobs, Climate Reports.

---

## 16. UI Plan

**Design only.**

| Screen | Purpose |
|--------|---------|
| **Climate Dashboard** | Job health, observation volume, quality grades, provider status |
| **Weather Station View** | Station metadata, map pin, recent series chart |
| **Observation Viewer** | Filterable table/chart by station, variable, time, quality |
| **Import Jobs** | Create/monitor jobs, download rejection samples |
| **Dataset Browser** | Catalog of published datasets and coverage |
| **Map Viewer** | Stations + footprints + bbox draw; link to station/scene |

Reuse existing enterprise layout, map stack (MapLibre), and RBAC patterns. No Claims/Insurance chrome on these pages.

---

## 17. Reporting Plan

| Report code | Description |
|-------------|-------------|
| `OBS_VOLUME_BY_DAY` | Ingest and accepted counts |
| `OBS_BY_PROVIDER` | Volume and reject rate |
| `STATION_COMPLETENESS` | Expected vs actual |
| `QUALITY_GRADE_SUMMARY` | Grade distribution |
| `IMPORT_JOB_OUTCOMES` | Success/fail/partial |
| `COVERAGE_BY_DISTRICT` | Spatial coverage proxy |
| `DATA_LAG` | Timeliness percentiles |

Reports read climate tables only — no join into claims amounts or premiums.

---

## 18. Integration Points

| Direction | Mechanism | Notes |
|-----------|-----------|-------|
| Climate → EventBus | `ClimateObservationAccepted`, `ClimateImportCompleted`, `ClimateQualityReportGenerated` | For 8B+ subscribers |
| Agriculture → Climate | Optional `farm_id` / bbox from farm geometry | Read farm geom; do not own farms |
| Insurance / Claims | **Read APIs or events only (later)** | No write into climate from claims |
| Object storage | Raster/scene binaries | Via storage SPI |
| Existing `008` tables | Evolve via Liquibase 018+ after approval | Separate Risk/VI ownership |

**Forbidden:** Claims assessment calling OpenWeather; Insurance premium engine writing observations; Settlement reading climate for payment.

---

## 19. Security Considerations

| Topic | Control |
|-------|---------|
| RBAC | `climate:read/write/import/admin`, `reports:climate` |
| Secrets | Provider API keys in vault; config stores reference only |
| PII | Climate data generally non-PII; farm linkage is business sensitive |
| Abuse | Mandatory time windows; max page size; rate limit import APIs |
| Integrity | Append-only facts; checksums on rasters; audit on catalog writes |
| Tenancy (future) | Provider/dataset scoping if multi-tenant government rollouts |

---

## 20. Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Unbounded time-series growth | DB cost / slow queries | Partitioning, aggregates, retention, query guards |
| Provider schema drift | Broken imports | SPI versioning, validation quarantine |
| Dual model (008 wide vs long) | Migration debt | Explicit cutover plan in implementation ADR |
| Raster-in-DB temptation | DB bloat | URI + COG only in v1 |
| Premature Risk coupling | Architecture violation | Keep `risk_scores` / VI out of climate module |
| Spatial invalid geometries | Query failures | Validation + topology checks |
| Over-fetch from commercial APIs | Cost | Job windows, cache, quotas |
| “Just one ML feature” scope creep | Delays 8A | Hard non-goals; separate 8B design gate |

---

## Quality Gate (this document)

Phase 8A **design** is ready for approval when:

- [x] Domain vision and non-goals clear (no AI / Claims / Insurance / Risk calc)
- [x] Bounded context and forbidden dependencies stated
- [x] Provider extension points listed (no implementations)
- [x] Entities designed: Station, Weather Observation, Satellite Observation, Raster Layer, Dataset, Provider, Import Job, Quality Report, Validation Result
- [x] PostGIS spatial strategy defined
- [x] Time-series partitioning / indexes / retention / aggregation designed
- [x] API, UI, reporting plans drafted (no code)
- [x] Risks and integration rules documented

**STOP.** No migrations, controllers, frontend, or provider adapters until this design is **explicitly approved**.

---

## Implementation preview (after approval only)

| Item | Planned |
|------|---------|
| Version | 0.13.0 |
| Liquibase | `018-climate-data-phase8a.yaml` (evolve `008`, add jobs/quality/datasets/providers) |
| Manual provider + CSV import | First vertical slice |
| Stub adapters | National Met, OpenWeather, Tomorrow.io, NASA, Copernicus, Sentinel, Planet, NOAA |
| Tests | Import/validation IT, observation query IT, spatial filter IT |

---

## Deliverables checklist (design gate response)

| # | Deliverable | Section |
|---|-------------|---------|
| 1 | Architecture Summary | §§1–3, 18 |
| 2 | Domain Model | §§5–7 |
| 3 | Data Model | §§5–7 |
| 4 | Spatial Strategy | §9 |
| 5 | Time-Series Strategy | §8, 13 |
| 6 | API Plan | §15 |
| 7 | UI Plan | §16 |
| 8 | Technical Risks | §20 |
