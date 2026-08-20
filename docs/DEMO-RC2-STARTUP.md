# AegisTerra local startup

**Release:** AegisTerra 1.0.0
**Verified runtime:** Java 21.0.12, Node.js 24.17.0, Docker Desktop with PostGIS 16-3.4

## 1. Clone or open the project

```powershell
cd "C:\Users\s.shyaka\Desktop\GRACE PROJECT\AegisTerra"
```

## 2. Start required infrastructure

```powershell
docker compose up -d
```

Expected services:
- PostGIS container on localhost:5432
- Database: `aegisterra`
- Database user: `aegisterra`
- Database password: `aegisterra`

## 3. Configure environment

The backend uses the local profile by default and will use the local PostGIS database. Optional environment overrides for a demo run:

```powershell
$env:AEGISTERRA_BOOTSTRAP_ADMIN_PASSWORD="Admin@1234!Aa"
$env:AEGISTERRA_JWT_SECRET="local-only-change-me-aegisterra-jwt-secret-key-32bytes-min"
```

Frontend defaults are already wired for the local dev proxy:
- `VITE_API_URL=`
- `VITE_DEV_API_PROXY=http://localhost:8080`

## 4. Start backend

```powershell
.
start-backend.ps1
```

Backend URL:
- http://localhost:8080/api/v1/health

## 5. Start frontend

Open a second terminal:

```powershell
.
start-frontend.ps1
```

Frontend URL:
- http://127.0.0.1:3000

## 6. Login

Primary demo account:
- Username: `admin`
- Password: `Admin@1234!Aa`

All other system roles share the local demo password `Demo@1234!Aa`:

| Username | Role |
|---|---|
| `insurance.admin` | INSURANCE_ADMIN |
| `insurance.officer` | INSURANCE_OFFICER |
| `fi.officer` | FI_OFFICER |
| `gov.analyst` | GOVERNMENT_ANALYST |
| `aggregator` | AGGREGATOR |
| `farmer.demo` | FARMER |
| `auditor` | AUDITOR |
| `support` | SUPPORT |

> DEMO / LOCAL ONLY: bootstrap accounts are only for local/demo use and must be disabled or replaced before production.

## 7. Verify the dashboard

After login, confirm that the executive overview loads with non-zero values for:
- Farmers
- Farms
- Policies
- Claims
- Climate alerts

## 8. Verify database and seed data

Confirm the database is reachable and the demo seed is present:

```powershell
docker compose ps
```

Then verify the API:

```powershell
.
scripts\demo-smoke.ps1
```

## 9. Run the smoke test

```powershell
.
scripts\demo-smoke.ps1
```

Expect the script to report `PASS` for the main demo endpoints.

## Troubleshooting

### Backend won't start
- Confirm Docker is running.
- Confirm Java 21 is installed.
- Confirm the database container is healthy.
- Re-run `docker compose up -d` and then restart the backend.

### Database unavailable
- Check `docker compose ps`.
- Re-run `docker compose up -d`.
- If the local database is corrupted, recreate it with `docker compose down -v` and then `docker compose up -d`.

### Frontend can't reach backend
- Confirm the backend is listening on http://localhost:8080.
- Confirm Vite proxy settings in [frontend/vite.config.ts](../frontend/vite.config.ts).
- Ensure the browser is using http://127.0.0.1:3000.

### Migration failure
- Review the backend startup logs for Liquibase errors.
- Reset the local PostGIS container only if the database state is clearly broken.

### Login failure
- Confirm the bootstrap password is set correctly for the local profile.
- Verify the backend has completed startup and the admin account was created.

### Empty dashboard
- Confirm the demo seed ran successfully.
- Re-run the backend startup once after the database is healthy.
- Re-run the smoke test to confirm the seeded portfolio appears.
