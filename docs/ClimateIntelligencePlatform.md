# AegisTerra Climate Intelligence & Risk Platform

**Version:** 1.0.0 (Climate Intelligence design gate)  
**Status:** **Implemented** in **0.14.0** (consumes Phase 8A Climate Data Platform)  
**Target release (implementation):** **0.14.0** (Phase 8B)  
**Depends on:** Phase 8A Climate Data Platform · Phase 4 Agriculture (farm geometry) · Phase 2 IAM · ArchitectureGovernance  
**Related:** `ClimateDataPlatform.md`, `ArchitectureGovernance.md`, `DevelopmentRoadmap.md`  

> **Hard rule:** This platform **consumes normalized climate observations** from the Climate Data Platform only.  
> It must **not** ingest raw provider feeds, call OpenWeather/NASA/etc., or embed Claims/Insurance/ML logic.

---

## 1. Domain Vision

The Climate Intelligence & Risk Platform turns trusted climate facts into **deterministic environmental intelligence**: farm and district risk scores, drought/flood/rainfall/vegetation indicators, seasonal summaries, climate timelines, and operator alerts.

It is a **read-compute-publish** bounded context:

1. **Read** validated observations, aggregates, stations, satellite metadata, and rasters via Climate Data APIs/events.
2. **Compute** rule-based indicators and scores (thresholds, windows, spatial joins — **no ML**).
3. **Persist** intelligence products (scores, alerts, profiles, summaries) with full lineage to source observation windows.
4. **Expose** APIs and dashboards for operators and (later) Insurance/Claims consumers.

**Non-goals (Phase 8B):**

- Machine learning, neural networks, predictive forecasting models.
- Fraud detection.
- Claims adjudication or payment logic.
- Insurance premium pricing or underwriting decisions (may *read* scores later under a separate gate).
- Direct external climate provider calls.
- Raw weather ingest / provider SPI (owned by 8A).

**Honest baseline:** Liquibase `008` includes placeholder `risk_scores` and `vegetation_indices` tables. Phase 8B **owns** those product concepts and will evolve them under climate-intelligence migrations after approval — Climate Data Platform does not calculate them.

---

## 2. Business Goals

| Goal | Outcome |
|------|---------|
| Actionable risk | Farm / district / national views of environmental stress |
| Deterministic & auditable | Same inputs + rules → same score; rule version recorded |
| Early warning | Climate alerts with severity and spatial scope |
| Seasonal awareness | Season summaries and trends for agricultural seasons |
| Reuse | Products callable by future Claims/Insurance without copying formulas |
| Separation of concerns | Data quality lives in 8A; intelligence lives in 8B |

---

## 3. Bounded Context

```text
 Climate Data Platform (8A)
   trusted observations · aggregates · stations · satellite · rasters
            │ read APIs / events only
            ▼
 ┌──────────────────────────────────────────────────────┐
 │     Climate Intelligence & Risk Platform (8B)        │
 │  Indicators · Risk Engine · Alerts · Profiles ·       │
 │  Timelines · Season Summaries · Spatial Analytics    │
 └───────────────┬───────────────────────┬──────────────┘
                 │ events / read APIs    │
                 ▼                       ▼
        Future Insurance/Claims    Operator UI / Reports
        (subscribe later)          (8B frontend)
```

| May depend on | Must not depend on |
|---------------|--------------------|
| Climate Data **read** APIs / published events | Climate Data **write**/import/provider SPI |
| Agriculture farm/season geometry (read) | Claims / Settlement / PaymentProvider |
| Identity RBAC + audit | External meteo/EO provider HTTP clients |
| EventBus (publish intelligence events) | Modifying Workflow/Task/Decision engines |
| PostGIS spatial functions | Training ML models |

**Package plan (implementation later):**

- `application/climateintelligence/*` — risk, drought, flood, rainfall, vegetation, season, alert, profile, reporting
- `domain/climateintelligence/*` — enums, rule versions, score grades
- `infrastructure/persistence/climateintelligence/*` — scores, alerts, profiles, indicator runs
- `presentation/*ClimateIntelligence*Controller` / Risk controllers

---

## 4. Domain Services

| Service | Responsibility |
|---------|----------------|
| `ClimateDataReadPort` | Anti-corruption layer over 8A observation/aggregate/station/satellite APIs |
| `RainfallAnalysisService` | Windowed rainfall totals, deficits, anomalies vs baselines |
| `DroughtDetectionService` | Drought indicator from rainfall + optional soil/temp series |
| `FloodDetectionService` | Flood stress from intense rainfall + optional spatial runoff proxies |
| `VegetationAnalysisService` | Rule-based VI stats from 8A satellite products / precomputed VI inputs **supplied as data**, not ML |
| `SeasonalTrendService` | Season-bounded trends and comparisons |
| `RiskEngine` | Orchestrates indicators → Farm / District / National risk scores |
| `ClimateAlertService` | Evaluates alert rules; creates/acknowledges alerts |
| `FarmClimateProfileService` | Historical climate profile per farm |
| `ClimateTimelineService` | Ordered climate events/indicators for a farm or district |
| `WeatherSummaryService` | Human-readable period summaries from aggregates |
| `SpatialAnalyticsService` | Zonal stats, district rollups, map layers |
| `IntelligenceJobService` | Batch recalculation jobs (nightly / on-demand) |

All formulas are **versioned rule sets** (`rule_set_code` + `rule_version`) stored as config — not hard-coded magic in controllers.

---

## 5. Risk Engine Architecture

```text
Climate Data aggregates (VALID)
        │
        ▼
 Indicator calculators (drought, flood, rainfall, vegetation, season)
        │
        ▼
 RiskEngine.compose(weights, window, geography)
        │
        ├── FarmRiskScore
        ├── DistrictRiskProfile
        └── NationalRiskSnapshot
        │
        ▼
 Persist + EventBus (FarmRiskScoreCalculated, …)
```

**Principles:**

1. **Inputs** are only 8A trusted aggregates/observations + farm/district geometry.
2. **Indicators** are dimensionless or physical metrics with documented units.
3. **Score** is 0–100 (or 0–1) with `confidence` from data completeness (from 8A quality / coverage).
4. **Lineage** stores: time window, station set / bbox, rule version, input aggregate ids or hashes.
5. **Idempotent jobs** recalculate by `(subject_type, subject_id, window, rule_version)`.
6. **No provider calls** inside the engine.

**Weighting (config):** e.g. drought 0.35, flood 0.25, rainfall anomaly 0.20, vegetation stress 0.20 — adjustable per season/crop zone without code change.

---

## 6. Drought Detection

| Element | Design |
|---------|--------|
| Inputs | Daily/rolling rainfall sums; optional temp max; completeness % |
| Methods (deterministic) | SPI-like z-score vs historical baseline **from platform aggregates**; threshold days below percentile; consecutive dry days |
| Outputs | `DroughtIndicator` (severity, index_value, window, geometry scope) |
| Baseline | Historical Climate Profile / district climatology tables maintained by 8B from 8A history |
| Non-goals | Predictive drought ML; satellite soil moisture fusion beyond rule thresholds |

Severity bands (example): `WATCH` / `WARNING` / `SEVERE` based on configurable cutoffs.

---

## 7. Flood Detection

| Element | Design |
|---------|--------|
| Inputs | Short-window rainfall intensity (1h/24h/72h); optional station density |
| Methods | Exceedance of intensity thresholds; accumulation over saturated windows |
| Spatial | Farm polygon intersection with district/basin layers if available; else centroid + radius |
| Outputs | `FloodIndicator` with severity and peak window |
| Non-goals | Hydraulic modeling, ML flood maps |

---

## 8. Rainfall Analysis

| Product | Description |
|---------|-------------|
| Period total | Sum over season / custom window |
| Anomaly | % of long-term mean for same calendar window |
| Distribution | Wet/dry day counts |
| Weather Summary | Narrative + metrics for UI (`WeatherSummary`) |

Consumes **8A aggregates** preferentially; falls back to observation scan only for small windows.

---

## 9. Vegetation Analysis

| Element | Design |
|---------|--------|
| Inputs | Satellite observation metadata + **index values** already stored as climate-adjacent facts (evolve `vegetation_indices` under 8B ownership) sourced from 8A satellite products via batch derivation jobs that apply **simple band-math formulas** (e.g. NDVI from provided reflectance layers) — not neural nets |
| Methods | Mean/min/max VI over window; departure from farm/district baseline |
| Outputs | `VegetationStressIndicator` |
| Boundary | If reflectance rasters are unavailable, service returns `INSUFFICIENT_DATA` — never calls Planet/Sentinel APIs |

---

## 10. Seasonal Trend Analysis

| Element | Design |
|---------|--------|
| Season binding | Agriculture `season` / crop calendar codes |
| Trends | Compare current season vs prior N seasons (rainfall, drought days, VI means) |
| Outputs | `SeasonSummary` (metrics JSON + grade + narrative keys) |
| Climate Timeline | Chronological mix of alerts + indicator peaks + significant weather windows for a farm |

---

## 11. Climate Alert Framework

| Attribute | Notes |
|-----------|--------|
| `alert_number` | e.g. `CLT-ALRT-2026-000000001` |
| `alert_type` | `DROUGHT`, `FLOOD`, `RAINFALL_EXTREME`, `VEGETATION_STRESS`, `DATA_GAP` |
| `severity` | `INFO`, `WATCH`, `WARNING`, `CRITICAL` |
| `scope_type` | `FARM`, `DISTRICT`, `NATIONAL`, `BBOX` |
| `scope_id` / `geom` | Target |
| `valid_from`, `valid_to` | Lifetime |
| `rule_version`, `evidence_json` | Audit |
| `status` | `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `EXPIRED` |

**Evaluation:** scheduled job + event-driven on `ClimateImportCompleted` / aggregate refresh (from 8A events).  
**Dedup:** one open alert per `(type, scope, severity band)` unless escalated.  
**Notifications:** via existing Communication Engine templates (no private mailer).

---

## 12. Farm Climate Profile

`HistoricalClimateProfile` / `FarmClimateProfile`:

| Section | Content |
|---------|---------|
| Identity | `farm_id`, generated_at, rule_version |
| Climatology | Monthly mean rainfall/temp from history |
| Extremes | Max dry spell, peak 24h rain, VI lows |
| Recent | Last 30/90 day summary |
| Risk | Latest Farm Risk Score + open alerts |
| Data quality | Completeness / lag from 8A |

Rebuilt periodically; stored as JSON snapshot + normalized metric rows for querying.

---

## 13. Spatial Analytics

| Capability | Design |
|------------|--------|
| Zonal stats | Aggregate indicators over farm polygon / district boundary |
| District rollup | Mean/P90 farm scores; alert counts |
| National dashboard | Choropleth-ready district metrics |
| Map layers | GeoJSON: farm score class, alert footprints, district grades |
| CRS | EPSG:4326; computations via PostGIS |

`SpatialAnalyticsService` never writes climate observations — only intelligence products and map DTOs.

---

## 14. Risk Scoring

### Farm Risk Score

| Field | Notes |
|-------|--------|
| `farm_id`, `score`, `confidence` | 0–100, 0–1 |
| `grade` | e.g. `LOW` / `MODERATE` / `HIGH` / `EXTREME` |
| `window_start`, `window_end` | |
| `components_json` | drought, flood, rainfall, vegetation contributions |
| `rule_set_code`, `rule_version` | |
| `model_version` | Synonym for rule pack id (non-ML) |
| `calculated_at` | |

### District Risk Profile

Rollup of farm scores + district-level indicators + open alerts.

### National Risk Dashboard

Snapshot metrics: farms by grade, open critical alerts, district heat list, data coverage %.

**Scoring algorithm (illustrative, config-driven):**

```text
score = 100 * clamp(
  w_d * drought_norm +
  w_f * flood_norm +
  w_r * rainfall_stress_norm +
  w_v * vegetation_stress_norm
)
confidence = f(observation_completeness, station_proximity, satellite_availability)
```

Normalization maps each indicator to 0–1 via piecewise thresholds — **no neural nets**.

---

## 15. Integration Points

| From → To | Mechanism | Rule |
|-----------|-----------|------|
| 8A → 8B | Read API + events (`ClimateObservationAccepted`, `ClimateImportCompleted`, `ClimateQualityReportGenerated`) | 8B never writes 8A facts |
| 8B → EventBus | `FarmRiskScoreCalculated`, `ClimateAlertRaised`, `ClimateAlertResolved`, `SeasonSummaryPublished` | For future Claims/Insurance |
| Agriculture → 8B | Farm/season IDs + geometry | Read-only |
| 8B → UI | REST | Operator tools |
| Claims/Insurance → 8B | **Later** read APIs only | No 8B knowledge of claim status |

**Forbidden:** Claims calling drought SPI; Insurance writing risk scores mid-quote without a future dedicated gate; 8B importing CSV weather files.

---

## 16. API Plan

**Design only — no implementation.**

Base: `/api/v1`  
Permissions (planned): `climate-intel:read`, `climate-intel:write`, `climate-intel:admin`, `alerts:climate`, `reports:climate-intel`

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/climate-intel/farms/{farmId}/risk-score` | Latest / by window |
| GET | `/climate-intel/farms/{farmId}/profile` | Historical climate profile |
| GET | `/climate-intel/farms/{farmId}/timeline` | Climate timeline |
| GET | `/climate-intel/farms/{farmId}/weather-summary` | Period weather summary |
| GET | `/climate-intel/farms/{farmId}/season-summary` | Season summary |
| GET | `/climate-intel/districts/{code}/risk-profile` | District profile |
| GET | `/climate-intel/national/dashboard` | National risk dashboard |
| GET | `/climate-intel/alerts` | Search alerts |
| GET | `/climate-intel/alerts/{id}` | Alert detail |
| POST | `/climate-intel/alerts/{id}/acknowledge` | Ack |
| POST | `/climate-intel/jobs/recalculate` | Trigger batch (`admin`) |
| GET | `/climate-intel/indicators` | Drought/flood/rainfall/VI indicators query |
| GET | `/climate-intel/map/risk` | GeoJSON risk layer |
| GET | `/climate-intel/reports/{name}` | Named reports |

OpenAPI tags: Climate Intelligence, Risk Scores, Climate Alerts, Climate Profiles, Climate Intel Reports.

---

## 17. UI Plan

**Design only.**

| Screen | Purpose |
|--------|---------|
| **National Risk Dashboard** | Grades, alerts, coverage, district heat |
| **District Risk View** | Profile + farm list + map |
| **Farm Risk / Profile** | Score, components, historical profile, timeline |
| **Climate Timeline** | Chronological indicators & alerts |
| **Alerts Inbox** | Filter, acknowledge, drill-down evidence |
| **Season Summary** | Per-farm or district season cards |
| **Recalculation Jobs** | Admin job monitor |

Reuse MapLibre + enterprise layout. Link to Climate Data UI for raw observations (read-only navigation), not duplicate ingest screens.

---

## 18. Reporting

| Report code | Description |
|-------------|-------------|
| `FARMS_BY_RISK_GRADE` | Counts and lists |
| `OPEN_ALERTS_BY_TYPE` | Alert portfolio |
| `DISTRICT_RISK_HEAT` | District scores |
| `DROUGHT_COVERAGE` | Farms/districts in drought bands |
| `SEASON_COMPARISON` | Season vs prior |
| `DATA_CONFIDENCE` | Low-confidence scores (data gaps) |
| `RECALC_JOB_OUTCOMES` | Batch health |

---

## 19. Security

| Topic | Control |
|-------|---------|
| RBAC | `climate-intel:*`, `alerts:climate`, `reports:climate-intel` |
| Authorization | Farm/district scoped reads for field officers; national for admins |
| Integrity | Append score history; never silently overwrite without version row |
| Abuse | Recalc jobs rate-limited; expensive spatial queries capped |
| Separation | No access to claim PII from this module |
| Audit | Score calculation and alert state changes audited |

---

## 20. Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Computing on raw obs instead of aggregates | Latency / DB load | Mandate aggregate-first; window guards |
| Hidden provider calls | Architecture break | No HTTP clients in intel module; fitness test |
| Ambiguous ownership of `008.risk_scores` | Dual writes | 8B owns product tables; document migration |
| Threshold tuning wars | Unstable scores | Versioned rule sets; change control |
| Insufficient climate data | False confidence | Confidence channel; `INSUFFICIENT_DATA` |
| Alert storms | Operator fatigue | Dedup, hysteresis, severity escalation rules |
| Scope creep into ML/Claims | Delay | Hard non-goals; separate future gates |
| Spatial join cost | Slow dashboards | Precomputed district rollups; materialized layers |

---

## Reusable output catalog

| Output | Description |
|--------|-------------|
| **Farm Risk Score** | Composite deterministic score for a farm/window |
| **Climate Timeline** | Time-ordered intelligence events for farm/district |
| **Season Summary** | Season-bounded metrics and grade |
| **Climate Alerts** | Typed, scoped, severity-managed alerts |
| **Weather Summary** | Period rainfall/temp narrative + metrics |
| **Historical Climate Profile** | Farm climatology + extremes + recent risk |
| **District Risk Profile** | District rollup + alerts |
| **National Risk Dashboard** | Country-level snapshot for operators |

---

## Quality Gate (this document)

Phase 8B **design** is ready for approval when:

- [x] Consumes Climate Data only (no raw ingest / no external providers)
- [x] No ML / neural nets / prediction models / fraud / Claims / Insurance logic
- [x] Risk engine, drought/flood/rainfall/vegetation/season/alerts/profiles designed
- [x] Spatial analytics and scoring model specified
- [x] API / UI / reporting / security / risks documented
- [x] Reusable outputs listed

**STOP.** No code, Liquibase, APIs, or frontend until **explicitly approved**.  
**Implementation order:** Climate Data Platform (8A) must be **implemented** before 8B coding begins.

---

## Implementation preview (after approval only)

| Item | Planned |
|------|---------|
| Version | 0.14.0 |
| Liquibase | `019-climate-intelligence-phase8b.yaml` (evolve `risk_scores`, `vegetation_indices`; add alerts, profiles, indicator runs, rule sets) |
| First slice | Rainfall analysis + Farm Risk Score + Alerts from aggregates |
| Tests | RiskEngine unit tests, farm score IT with fixture aggregates, alert dedup tests |
