# Backend setup

Spring Boot 3.3 modular-monolith foundation for AegisTerra (**0.4.0** — Phase 3 Enterprise Data Platform).

## Prerequisites

- Java 21+
- Docker (required for local PostgreSQL/PostGIS and for tests)
- Maven is **optional** — use the included wrapper (`mvnw.cmd`)

## Database (PostGIS)

From the `AegisTerra` directory:

```powershell
docker compose up -d
```

This starts `postgis/postgis:16-3.4` with:

| Setting | Value |
|---------|-------|
| Database | `aegisterra` |
| User / password | `aegisterra` / `aegisterra` |
| Port | `5432` |

Stop with `docker compose down`.

## Profiles

| Profile | Purpose |
|---------|---------|
| `local` (default) | PostgreSQL + PostGIS on localhost:5432 |
| `local-h2` | Optional H2 IAM-only quick start (no spatial) |
| `test` | Automated tests; datasource overridden by Testcontainers |
| `prod` | Production PostgreSQL via env vars; bootstrap disabled |

```powershell
# Optional IAM-only without Docker DB
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-h2"
```

## Run locally

From the `AegisTerra` directory:

```powershell
docker compose up -d
.\start-backend.ps1
```

Or from `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

API docs: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/api/v1/health

Bootstrap admin (local): `admin` / `Admin@1234!Aa` (override with `AEGISTERRA_BOOTSTRAP_ADMIN_PASSWORD`).

## Test

Requires Docker (Testcontainers PostGIS):

```powershell
.\mvnw.cmd -B test
```

## Package layout

- `domain` — business model / identity enums
- `application` — use cases (identity only in Phase 2/3)
- `infrastructure/persistence` — JPA entities + repositories by bounded context
- `infrastructure/security` — JWT, cookies, filters
- `presentation` — HTTP controllers / DTOs (domain controllers still scaffolds)
- `config` — Spring configuration
- `shared` — cross-cutting (errors, correlation id)

## Phase 3 note

Schema, entities, and repositories for geography, party, agriculture, insurance, climate, engagement, and platform are in place. Farmer Management APIs and workflows are **Phase 4** — not implemented here.
