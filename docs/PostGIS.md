# AegisTerra PostGIS Architecture

**Version:** 3.0.0  
**Depends on:** ADR-001, `DataArchitecture.md`

---

## 1. Extension

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

Applied via Liquibase changeset before spatial columns.

Verify: `SELECT PostGIS_Version();`

---

## 2. CRS and types

| Choice | Value | Rationale |
|--------|-------|-----------|
| Storage SRID | **4326** (WGS84) | Global interchange, GeoJSON default |
| Farm / admin boundaries | `geometry(MultiPolygon, 4326)` | National parcels & admin units |
| Optional single ring | Accept Polygon; normalize to MultiPolygon on write | Simpler client payloads |
| Stations / points | `geometry(Point, 4326)` | Weather stations, centroids |
| Area / distance | Prefer `geography` casts or `geometry` projected ops | `ST_Area(geom::geography)` → m² |

Phase 3 stores **geometry** with SRID 4326. Application helpers expose area in hectares.

---

## 3. Tables with geometry (Phase 3)

| Table | Column | Type |
|-------|--------|------|
| `farm_boundaries` | `geom` | MultiPolygon, 4326 |
| `plots` | `geom` | MultiPolygon, 4326, nullable until surveyed |
| `districts` / `sectors` / `cells` / `villages` | `geom` | MultiPolygon, nullable |
| `weather_stations` | `geom` | Point, 4326, NOT NULL |
| `satellite_observations` | `footprint` | MultiPolygon, nullable |

---

## 4. Indexes

```sql
CREATE INDEX idx_farm_boundaries_geom ON farm_boundaries USING GIST (geom);
CREATE INDEX idx_plots_geom ON plots USING GIST (geom);
CREATE INDEX idx_weather_stations_geom ON weather_stations USING GIST (geom);
-- similarly for admin boundaries and satellite footprints when populated
```

---

## 5. Validation rules

1. `ST_IsValid(geom)` must be true for ACTIVE boundaries.  
2. Reject empty geometries.  
3. Prefer `ST_MakeValid` only in controlled ETL — not silent on user digitizing errors.  
4. Coordinates: lon ∈ [-180,180], lat ∈ [-90,90].  
5. Rwanda operational bbox soft-check in app (configurable), not hard DB check (diaspora/test data).

---

## 6. Core spatial queries (repository-level)

| Need | Approach |
|------|----------|
| Farms in district | `ST_Within(boundary.geom, district.geom)` or admin FK |
| Farms near point | `ST_DWithin(geom::geography, point::geography, meters)` |
| Area ha | `ST_Area(geom::geography) / 10000.0` |
| Intersection with satellite footprint | `ST_Intersects` |
| GeoJSON out | `ST_AsGeoJSON(geom)` |

Phase 3 repositories expose these as Spring Data `@Query` methods where useful for tests; full GIS API is later.

---

## 7. GeoJSON interchange

- Input: Feature/Polygon/MultiPolygon → JTS → entity  
- Output: `ST_AsGeoJSON` or JTS writer  
- Do not store GeoJSON text as source of truth when `geom` exists

---

## 8. Future satellite overlays

- Store product id, acquisition time, cloud cover, `storage_uri`, optional `footprint`  
- Join to farms via intersects or precomputed `farm_id`  
- Vegetation indices reference observation + farm + index type + value

---

## 9. Local / CI

- **Docker Compose:** `postgis/postgis:16-3.4` (or current LTS)  
- **Tests:** Testcontainers with same image  
- H2 is **not** used for spatial Phase 3 tests

---

## 10. Operational notes

- Vacuum/analyze after bulk spatial loads  
- Reindex GIST if bloat detected  
- Backup/restore must include PostGIS extension
