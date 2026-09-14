# Product focus (ADR-010)

AegisTerra’s active product is **climate and yield intelligence for planning**.

## Owner need

Insurers and other stakeholders rely on one place to:

1. Learn from **past** climate and yield behaviour  
2. See **current** risk  
3. Plan for what is **coming** — so harvest-loss payouts are less unexpected  

The platform does **not** sell insurance and does **not** need a full claims/settlement company in the product UI.

## In product

- Identity / users  
- Farmers, farms, crops, seasons, crop history  
- Geography / AEZ  
- Climate data + climate intelligence (farm, district, AEZ, national)  
- Alerts  
- **Planning outlook** (`/planning`) — past → now → ahead  

## Frozen (not in default product)

Claims, settlements, ledger, payment providers, policy admin, premium calculator, workflow/tasks desks, lending UI, insured inputs, satellite stub, **GIS / climate map / boundary map editor**, farmer guidance UI, notifications as primary nav.

- SPA: routes redirect to planning/core  
- API: controllers require `aegisterra.modules.partner-ops=true` (enabled in **test** profile only by default)

## Next depth for 100% owner story

**Done (035):** `crop_seasons.yield_t_ha`, historical seasons 2016A–2024A, demo yield series, and
`GET /api/v1/climate-intel/planning/yield-outlook` (past decade → recent 2 years → predicted yield under current climate risk).

**ML Stage A (036 + ADR-011):** maize feature extract + leave-one-year-out metrics at
`GET /api/v1/climate-intel/planning/ml-spike/maize` — experimental only; rule outlook stays default.

Optional later: Stage B batch inference if linear beats the rule on **real** data.

## Final presentation

See [FinalPresentation.md](./FinalPresentation.md). Seed is complete enough that all active product paths work; real data replaces rows only (migration `037` backfills demo farm districts + stakeholder climate permissions).
