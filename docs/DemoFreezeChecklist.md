# Demo Environment Freeze Checklist (RC 1.0)

Use this the morning of the demo. After freeze: **no feature work** unless a smoke check fails.

## A. Start (known-good)

```powershell
cd "C:\Users\s.shyaka\Desktop\GRACE PROJECT\AegisTerra"
docker compose up -d
.\start-backend.ps1
# other terminal:
.\start-frontend.ps1
```

Wait until:

- http://localhost:8080/api/v1/health → OK  
- http://127.0.0.1:3000 → login page  

Admin: `admin` / `Admin@1234!Aa`

## B. API smoke (automated)

```powershell
cd "C:\Users\s.shyaka\Desktop\GRACE PROJECT\AegisTerra"
.\scripts\demo-smoke.ps1
```

Expect all lines `PASS`. If any `FAIL`, fix only that path, then re-run.

## C. UI smoke (90 seconds)

- [ ] Login succeeds  
- [ ] Overview shows non-zero farmers/farms/policies (RC1 seed)  
- [ ] Farmers search `FRM-RC1` returns rows  
- [ ] Policies search `POL-RC1` returns rows  
- [ ] Claims search `CLM-RC1` returns rows  
- [ ] Settlements list shows `SET-RC1`  
- [ ] Climate alerts shows open alerts  
- [ ] GIS loads without crash  
- [ ] Settings shows Platform information (not blank filler)  

## D. Freeze

- [ ] Note start time and machine  
- [ ] Do not pull new commits / run migrations mid-session unless recovering a failure  
- [ ] Keep `docs/DemoDressRehearsal.md` open on a second screen  
- [ ] Browser: one clean window, zoom 100–110%, hide bookmarks bar if possible  

## E. Recovery

```powershell
# Backend only
# Ctrl+C in backend terminal, then:
.\start-backend.ps1

# Database (last resort — destroys local data)
# docker compose down -v
# docker compose up -d
# then restart backend (Liquibase + RC1 seed re-apply)
```

## F. Post-demo

Only after audience leaves: capture issues in `docs/TechnicalDebt.md` or a short notes file. Do not hot-patch during applause.
