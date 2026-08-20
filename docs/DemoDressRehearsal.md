# AegisTerra Demo Dress Rehearsal Pack (RC 1.0)

**Release:** `1.0.0-rc.2`  
**Audience:** Presenter + backup operator  
**Goal:** Impress in the first five minutes without improvising

---

## Credentials

| Item | Value |
|------|-------|
| URL | http://127.0.0.1:3000 |
| API | http://localhost:8080 |
| User | `admin` |
| Password | `Admin@1234!Aa` |

Override password only via `AEGISTERRA_BOOTSTRAP_ADMIN_PASSWORD` before first admin bootstrap.

---

## Five-minute script (say this / click this)

### 0:00–0:40 — Login + national command
1. Open login → sign in as admin.  
2. Land on **Overview**.  
3. **Say:** “This is the national operations picture — farmers, farms, policies, claims, settlements, and climate risk in one command view.”  
4. Point to KPIs, risk grade mix, open alerts, unread notifications.

**Backup if overview API fails:** use nav → Climate intel + Settlements dash (modules still work).

### 0:40–1:30 — Agriculture + GIS
1. **Farmers** → search `FRM-RC1` → open **Jean Niyonzima** (`FRM-RC1-001`).  
2. **Farms** → open `FARM-RC1-001` (Niyonzima Maize Plot).  
3. **GIS** → enable Stations + Risk layers.  
4. **Say:** “Registered farmers and farm boundaries are the foundation for underwriting and parametric climate intelligence.”

### 1:30–2:40 — Insurance + claims
1. **Policies** → search `POL-RC1` → open `POL-RC1-2026-001` (ACTIVE).  
2. Mention draft `POL-RC1-2026-004` if asked about underwriting pipeline.  
3. **Claims** → search `CLM-RC1` → open drought claim `CLM-RC1-2026-001` (APPROVED).  
4. Glance at `CLM-RC1-2026-002` (SUBMITTED) to show pipeline depth.  
5. **Say:** “End-to-end insurance operations — product, policy, claim lifecycle — with workflow-ready status.”

### 2:40–3:40 — Settlement
1. **Settle. dash** → show pending vs completed posture.  
2. **Settlements** → open `SET-RC1-2026-001` (COMPLETED) and `SET-RC1-2026-002` (PENDING).  
3. Optional: **Settle. reports** → Export CSV.  
4. **Say:** “Approved claims move into a governed settlement and ledger path — finance controls, not a black box payout.”

### 3:40–5:00 — Climate intelligence close
1. **Climate intel** → national risk snapshot.  
2. **Climate alerts** → open `ALT-RC1-2026-001` (CRITICAL drought).  
3. Optional: **Climate data** → stations `DEMO-KGL-01` / `DEMO-MUS-01`.  
4. **Say:** “Climate Data is the system of record; Climate Intelligence turns it into deterministic risk and operator alerts for national programs.”

**Close:** return to **Overview** — “One platform for registration, coverage, claims, settlement, and climate risk.”

---

## Seed cheat sheet (RC1)

| Entity | Codes / IDs to search |
|--------|------------------------|
| Farmers | `FRM-RC1-001` … `008` |
| Farms | `FARM-RC1-001` … `012` |
| Policies | `POL-RC1-2026-001` … `006` |
| Claims | `CLM-RC1-2026-001` … `004` |
| Settlements | `SET-RC1-2026-001` (COMPLETED), `SET-RC1-2026-002` (PENDING) |
| Alerts | `ALT-RC1-2026-001` (CRITICAL), `002`–`004` |
| Stations | `DEMO-KGL-01`, `DEMO-MUS-01`, `DEMO-HUE-01`, `DEMO-NYG-01` |
| Districts | `KIGALI`, `MUSANZE`, `HUYE`, `NYAGATARE` |

Hero story line: **Jean Niyonzima** → maize farm → active policy → approved drought claim → completed settlement → critical drought alert.

---

## If something breaks mid-demo

| Symptom | Move |
|---------|------|
| Overview KPI error banner | Continue with module dashboards |
| Empty farmers/policies | Confirm Liquibase `020` ran; restart backend once |
| Map blank | Toggle Stations layer; zoom Rwanda (~Kigali) |
| Settlement empty | Open claim `CLM-RC1-2026-001` and tell the settlement story from claim status |
| Alert page empty | Climate intel national dashboard still shows risk grades |

Do **not** invent live weather API or bank payment demos — those are stubs by design.

---

## Talking points (30 seconds each)

- **Government:** national visibility + governed workflows for agricultural insurance programs.  
- **Insurer:** underwriting → claims → settlement with audit-friendly status history.  
- **Investor / judges:** modular monolith, production architecture, climate intelligence without fake AI slides.  
- **Honest scope:** Manual climate ingest + Manual settlement provider are operational; external providers are stubbed for later phases.
