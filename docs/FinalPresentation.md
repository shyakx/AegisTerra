# Final product presentation guide

AegisTerra is presented as **climate & yield intelligence for planning** (ADR-010).  
Seed data drives the demo; **replacing seed with real rows does not require code changes** — same APIs, same screens.

## Principle

| Layer | Behavior |
|---|---|
| UI / APIs | Read tables and permissions only — no hard-coded yield/climate numbers in product logic |
| Liquibase seed | Demo stand-in for real farms, yields, climate, districts |
| Real cutover | Upsert/replace `farms`, `crop_seasons.yield_t_ha`, `weather_*`, `districts` / AEZ refs |

## Run for live presentation (recommended)

1. Start PostGIS + backend (`application-local` / Docker as usual).  
2. Frontend against the API (not static demo stubs):

```env
VITE_DEMO_MODE=false
VITE_API_URL=
VITE_DEV_API_PROXY=http://localhost:8080
```

3. Log in as a demo stakeholder (e.g. insurance admin / government / farmer).  
4. Walk the path: **Overview → Planning outlook → Climate risk → Farms / crop history → Export CSV**.

## Active product paths (must work on seed)

- Login / roles  
- Dashboard + **Planning outlook** (past / now / ahead + maize ML spike panel)  
- National / district / AEZ risk  
- Climate stations & observations  
- Farmers / farms (with **district → AEZ**)  
- Farm climate risk page + crop history with yields  
- Alerts (climate risk)
- Planning / registry / climate data paths above  
- CSV export of yield outlook  

Frozen (not in presentation nav): claims, settlements, policies, lending, tasks desks.

## Seed migrations that make the story complete

| Migration | Role |
|---|---|
| 033 | Provinces, AEZ, 30 districts |
| 034 | Farm → district FK |
| 035 | Yield column + historical seasons + demo yields |
| 036 | Demo season rain for ML spike |
| 037 | Farm/farmer district backfill + climate-intel for all stakeholder roles |

## Replacing with real data

1. Keep reference geography/AEZ (or replace with official codes — same schema).  
2. Load real farmers/farms with `district_id`.  
3. Load `crop_seasons` with `yield_t_ha` and season dates.  
4. Load climate observations / aggregates from national met.  
5. Recalculate climate-intel risk.  
6. Planning + CSV continue to work unchanged.
