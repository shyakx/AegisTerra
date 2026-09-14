# ADR-009: Administrative Geography vs Agroecological Classification

## Status

Accepted

## Date

2026-09-04

## Context

AegisTerra already has an administrative geography schema (`districts` → `sectors` → `cells` → `villages`) with no Province entity, no geography HTTP API, and no agroecological model. The tables are empty after migrate.

An authoritative workbook (`Agroecological zones & Subzones.xlsx`) establishes Rwanda reference data for this phase:

- 5 agroecological zones (codes `A`–`E`)
- 18 agroecological sub-zones (codes `A1`–`A3`, `B1`–`B4`, `C1`–`C4`, `D1`–`D4`, `E1`–`E3`)
- 30 districts, each appearing once
- each district belongs to exactly one sub-zone in this dataset
- each sub-zone contains between 1 and 3 districts

The workbook also labels a province/group for each zone (`Northern Province`, `Southern Province`, `Eastern Province`, `Western Province`, `Kigali City`). In this file those labels align with ZONE A–E, but that alignment is a property of the current dataset, not an official identity between “ZONE A” and “Northern Province.”

The workbook contains **no AEZ polygons**. Farmer-sheet sector/cell values are not a trustworthy gazetteer (placeholder/duplicate behavior). The 450 “Registered Farmers” rows are synthetic (repeated names) and must not become production farmer identities.

Climate Intelligence currently persists `districtCode` as a varchar (`MUSANZE`, `HUYE`, …) and `RiskEngine` can derive it from `farm.getDistrictId().toString()` (a UUID) or fall back to a weather station. That identifier mismatch is out of scope for this ADR’s implementation phase.

## Decision

1. **Treat administrative geography and agroecological classification as parallel concepts**, not as a single hierarchy.

   ```text
   ADMINISTRATIVE GEOGRAPHY              AGROECOLOGICAL CLASSIFICATION
   Province                              Zone (A–E)
     └── District                          └── Sub-zone (A1…E3)
           └── Sector                            └── District  (N:1 in this dataset)
                 └── Cell
                       └── Village
   ```

2. **Administrative geography remains authoritative** for address, location, and KYC. Province is added above District. Sector/Cell/Village tables stay in place for a future official gazetteer and are **not** seeded from the farmer workbook.

3. **Agroecological classification is a separate reference-data layer** owned by the Geography bounded context. Agriculture consumes it through application/domain boundaries; it does not duplicate AEZ tables. Farmers themselves must not own AEZ classification. Farms will eventually **derive** AEZ from their administrative geography (district → sub-zone → zone).

4. **Current workbook cardinality is N:1 district → sub-zone.** Implement `districts.agroecological_subzone_id` → `agroecological_subzones.id` (no many-to-many junction in Phase 1). The domain must remain able to evolve: a future official zoning scheme may assign a district more than one classification or require **versioned** zoning (effective-from / superseded-by). A junction table or zoning-version table can be introduced then without rewriting address geography.

5. **Stable codes**

   | Kind | Codes |
   |------|--------|
   | Province | `NORTH`, `SOUTH`, `EAST`, `WEST`, `KIGALI` |
   | Zone | `A`, `B`, `C`, `D`, `E` (names `ZONE A` … `ZONE E`) |
   | Sub-zone | `A1`–`A3`, `B1`–`B4`, `C1`–`C4`, `D1`–`D4`, `E1`–`E3` |
   | District | uppercase official-style names (`MUSANZE`, `HUYE`, …) matching Climate station `district_code` style |

   Province `KIGALI` is **Kigali City** (Gasabo, Nyarugenge, Kicukiro). It is not a 31st district and is not synonymous with climate station varchar `KIGALI`.

6. **Do not assert official synonymy** between ZONE A and Northern Province (or B/South, C/East, D/West, E/Kigali City). They are aligned in this workbook only; store both, relate districts independently to `province_id` and `agroecological_subzone_id`.

7. **Spatial AEZ assignment is not part of Phase 1.** Excel has no polygons. Do not fabricate MultiPolygons. Keep existing SRID 4326 / PostGIS farm geometry unchanged. `geom` on provinces/districts stays nullable.

8. **Climate boundaries**

   - Do **not** put AEZ logic into Climate Data 8A.
   - Climate Intelligence 8B may **consume** the classification as a **read-time** Zone/Sub-zone rollup over latest `district_risk_snapshots` joined to `districts.code` → sub-zone → zone (Phase 3). This does **not** change 8A/8B formulas, does **not** rewrite historical snapshots, and does **not** invent AEZ membership for unmapped station labels such as `KIGALI`.
   - **Phase 2 (done):** Climate `districtCode` resolves through `districts.code`, not `UUID.toString()`.

9. **Reference data is migration-controlled** in this phase (read APIs only; no write/admin geography APIs; no new `aez:read` permission).

10. **Do not import** the 450 workbook farmers, do not seed fake sector/cell/village rows, and do not modify Claims or Settlement.

## Consequences

### Positive

- Address/KYC geography and agroecological risk geography can evolve independently
- District codes align with existing climate varchar identifiers (`MUSANZE`, `HUYE`, `NYAGATARE`)
- A single FK matches the authoritative N:1 dataset without a speculative junction table
- Empty Province gap in the current schema is closed without replacing `districts`

### Negative / costs

- ZONE A ↔ Northern Province coincidence may be misread as official; APIs and docs must keep codes/names separate
- N:1 FK will need a migration if a future gazetteer is M:N or versioned
- Climate `districtCode` UUID leak was fixed in Phase 2 (`RiskEngine` → `districts.code`)
- Climate station code `KIGALI` still does not map 1:1 to a district row (Kigali City has three districts); Phase 3 AEZ rollups **exclude** unmapped codes rather than inventing membership
- National district heat and AEZ aggregation both use latest-per-district snapshots (`DISTINCT ON (district_code) … ORDER BY calculated_at DESC`); unmapped codes remain listed separately and are never invented into AEZ membership

### Implementation notes (Phase 1)

- Liquibase `032` (schema) and `033` (idempotent seed): 5 provinces, 5 zones, 18 sub-zones, 30 districts, 30 district→province and 30 district→sub-zone mappings
- Canonical sub-zone names come from the workbook **Sub-zone Summary** sheet (not farmer-sheet strings such as `Sb-zone C1_Eastern Dry Savanna`)
- Geometry is not populated

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Collapse AEZ into administrative geography (zone = province) | Workbook alignment is coincidental; official AEZ ≠ province |
| Many-to-many `district_agroecological_subzones` now | Dataset is strictly N:1; junction is unused complexity |
| Store AEZ on farmer/farm rows | Farmers must not own classification; farms derive it later from geography |
| Seed sectors/cells/villages from the farmer sheet | Not a complete or trustworthy gazetteer |
| Spatial AEZ assignment / choropleths in Phase 1 | Workbook has no polygons |
| AEZ formulas or snapshots inside Climate 8A/8B in Phase 1 | Reference-data foundation first |
| New `aez:read` permission | Existing `farmers:read` already covers KYC/registry catalog reads |
