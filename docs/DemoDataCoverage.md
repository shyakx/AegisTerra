# AegisTerra Demo Data Coverage Report

## Scope

This document inventories the current demo-data coverage for the AegisTerra RC2 baseline without changing product behavior or introducing new business logic. The focus is discovery and readiness assessment only.

## Verified evidence from the current environment

The current demo stack was verified with the existing smoke script and live backend calls:

- Executive overview returned: farmers = 9, farms = 13, climate stations = 4, open alerts = 1
- Farmers search returned 8 rows matching the RC1 demo prefix
- Policies returned 6 rows
- Claims returned 4 rows
- Settlements returned 2 rows
- Notifications returned 3 rows
- User management returned 1 active user
- Climate intelligence returned 4 district rollups and 4 alerts

These values are sufficient to support a controlled demo, but several screens still need more complete data and richer narrative context.

## Screen classification summary

| Screen | Classification | Why |
|---|---|---|
| Executive Dashboard | A | Already populated and tied to real backend counts |
| Farmers | A | Seeded and searchable |
| Farms | A | Seeded and linked to boundaries |
| Policies | A | Seeded with realistic lifecycle values |
| Claims | A | Seeded across multiple claim states |
| Settlements | A | Seeded with pending and completed examples |
| Climate Intelligence / Risk Dashboard | A | Seeded with district and farm-risk data |
| GIS | A | Station and risk layers are available |
| Notifications | B | Notifications exist, but the inbox is still thin |
| Insurance Products | B | Product catalog exists, but package depth is limited |
| Premium Calculator | B | The UI works, but the demo story would be stronger with more product/package combinations |
| Households | B | Households exist, but they are not yet used as a visual storytelling layer |
| Plots | C | The route exists, but the seeded plot portfolio is still sparse |
| Claims Inspection / Assessment | C | The workflow pages are available, but the seeded detail data is minimal |
| Workflow Inbox / Tasks | C | Tasks are empty in the current live environment |
| Reports | B | Reports exist structurally, but the current seeded sample is still narrow |
| User Management | B | Users exist, but audit and role depth are limited |
| Audit Logs | C | The API exists but there is not yet a rich audit trail to show |
| Climate Datasets / Observations | C | The platform supports them, but the demo seed is not yet rich enough to make these pages compelling |
| Farm Risk Profile / District Risk Detail | C | The risk engine exists, but the drill-down screens need more context |
| Decisions | D | The decision API exists, but there is no clear demo narrative or populated decision surface |

## Inventory of important screens

| Screen | Route | Backend API | Database entities | Current record count | Data exists | Meaningful | What is missing | Recommended seed data | Priority |
|---|---|---|---|---|---|---|---|---|---|
| Executive Dashboard | / | /api/v1/executive/overview | farmers, farms, insurance_policies, claims, settlements, weather_stations, climate_alerts, risk_scores, workflow_tasks, notifications | Farmers 9, farms 13, policies 6, claims 4, settlements 2, alerts 4, stations 4 | Yes | Yes | Regional variation and richer settlement totals | Add more districts, more open claims, one or two more high-risk regions, and settlement value totals that tell a more nuanced story | High |
| National Overview | /climate-intel | /api/v1/climate-intel/national/dashboard | district_risk_snapshots, risk_scores, climate_alerts | 4 district snapshots, 4 alerts | Yes | Yes | A stronger national story with 5–7 districts and multiple risk grades | Add 2–3 additional districts with different severity patterns | High |
| Regional / District Dashboard | /climate-intel/districts/:code | /api/v1/climate-intel/districts/{code}/risk-profile | district_risk_snapshots, risk_scores, climate_alerts, farms | 4 districts seeded | Partial | Partially | Farm-level rollup detail and trend context | Seed 4–6 farm profiles per district, plus current-season risk indicators | High |
| Farmers | /farmers | /api/v1/farmers | farmers, households | 9 total, 8 matching demo prefix | Yes | Yes | More demographic and geographic spread beyond the current RC1-style seed | Add 8–12 more synthetic farmers across 3–4 districts with varied crop mixes | High |
| Households | /households | /api/v1/households | households | 8 | Yes | Partially | Households are present but not used to tell a household-to-farmer-to-farm story | Add 4–6 additional households linked to the new farmers | Medium |
| Farms | /farms | /api/v1/farms | farms, farm_boundaries | 13 | Yes | Yes | More crop and size diversity | Add 6–8 farms across different districts and crop types | High |
| Plots | /farms/:id/plots | /api/v1/plots | plots | Sparse/undefined from current live run | Partial | No | The screen is likely to feel empty during demonstration | Seed 2–3 plots for several farms with different sizes and status | Medium |
| GIS | /gis | /api/v1/climate/map/stations and /api/v1/climate-intel/map/risk | weather_stations, risk_scores, farm_boundaries | 4 stations and risk features | Yes | Yes | A more visually rich map needs more stations and district coverage | Add 2–4 more stations and risk points across northern, eastern, and southern districts | High |
| Insurance Products | /insurance/products | /api/v1/insurance-products, /api/v1/policy-types, /api/v1/coverage-packages | insurance_products, policy_types, coverage_packages | 1 product, 1 policy type, 1 package surfaced by current seed | Yes | Yes | The screen is too thin for a judge to understand the product catalog | Seed 2–3 additional packages and 1–2 additional policy types | Medium |
| Policies | /policies | /api/v1/policies | insurance_policies | 6 | Yes | Yes | More transition states and a spread of status values | Add 2–4 additional policies in DRAFT, PREMIUM_PENDING, and EXPIRED states | High |
| Premium Calculator | /insurance/calculator | /api/v1/premiums/quote | insurance_products, coverage_packages, farms, farmers, pricing rules | Works with seeded data | Yes | Partially | The flow is real but not yet visually rich enough for a live demo | Add 2–3 packages and seed at least 3 farms with varying area and risk factors | Medium |
| Claims Dashboard | /claims | /api/v1/claims | claims | 4 | Yes | Yes | More claims states and one or two claims with inspection/assessment activity would improve the story | Add 2–4 additional claims in UNDER_VALIDATION and PAYMENT_PENDING | High |
| Claims Details | /claims/:id | /api/v1/claims/{id} | claims, claim_status_history | 4 | Yes | Partially | Details need richer evidence and a more complete lifecycle narrative | Seed one claim with evidence, one with assessment notes, one with decision history | High |
| Claims Inspection | /claims/:id/inspection | /api/v1/claims/{id}/inspections | claim_inspections | Not directly populated in current live evidence | Partial | No | The screen will feel empty without seeded inspection data | Seed 1–2 inspections with checklist and findings JSON | Medium |
| Claims Assessment | /claims/:id/assessment | /api/v1/claims/{id}/assessments | claim_assessments | Not directly populated in current live evidence | Partial | No | The assessment page needs a concrete example to demonstrate underwriting rigor | Seed 1–2 assessments with recommended and assessed amounts | Medium |
| Settlement Dashboard | /settlements/dashboard | /api/v1/settlements | settlements | 2 | Yes | Yes | It would be stronger with at least one pending and one completed example plus provider variation | Add one more pending settlement and one failed/rejected example | High |
| Settlement Details | /settlements/:id | /api/v1/settlements/{id} | settlements, settlement_status_history | 2 | Yes | Partially | Needs more narrative context and linked workflow history | Seed one detail record with workflow link and structured financial snapshot | Medium |
| Ledger | /ledger | /api/v1/ledger | ledger_entries, ledger_transactions | Not fully surfaced from the current environment | Partial | No | The page will appear incomplete unless linked settlement entries are present | Seed 4–6 ledger entries linked to settlements | Medium |
| Workflow Inbox | /tasks | /api/v1/tasks | workflow_tasks | 0 in the current live check | No | No | This is a high-visibility gap for demo storytelling | Seed 4–6 tasks across validation, inspection, settlement review, and finance review | High |
| Tasks | /tasks/:id | /api/v1/tasks/{id} | workflow_tasks | 0 in current run | No | No | The task detail path will be empty without workflow subjects | Seed at least 2 detailed tasks with subject references and assignee roles | High |
| Decisions | Not currently surfaced as a dedicated UI page | /api/v1/decisions/types and /api/v1/tasks/{id}/decisions | workflow_decision_records, workflow_tasks | Not visible from current run | No | No | The decision flow is conceptually important but currently not present in the demo experience | Seed 2–3 decision records mapped to claims or settlements | Medium |
| Notifications | /notifications | /api/v1/notifications | notifications | 3 | Yes | Yes | The current notifications are too few and not fully role-based | Add 3–5 more notifications covering policy approval, claim submission, risk alert, and settlement review | Medium |
| Climate Dashboard | /climate | /api/v1/climate/dashboard | weather_stations, weather_observations, climate_import_jobs, climate_datasets | Stations 4, datasets 0, imports 0 | Partial | Partially | The climate pages need richer import and dataset history | Add 2–3 datasets and 1–2 import jobs with accepted rows | Medium |
| Weather Stations | /climate/stations | /api/v1/climate/stations | weather_stations | 4 | Yes | Yes | More district spread is needed | Add 2 more stations in eastern and western districts | High |
| Observations | /climate/observations | /api/v1/climate/observations | weather_observations | Seeded but not yet surfaced as a rich demo series | Yes | Partially | The page needs a wider date window and more variable diversity | Add a 30-day observation series for at least 4 stations | Medium |
| Climate Datasets | /climate/datasets | /api/v1/climate/datasets | climate_datasets | 0 | No | No | The screen will look empty | Seed 2–3 datasets with official-style metadata and synthetic demo labels | Medium |
| Climate Intelligence | /climate-intel | /api/v1/climate-intel/national/dashboard | climate_alerts, risk_scores, district_risk_snapshots | 4 alerts, 4 district snapshots | Yes | Yes | More severity variety and regional spread would elevate the presentation | Add 2 high-severity alerts and 2 moderate alerts in different districts | High |
| Risk Dashboard | /climate-intel | /api/v1/climate-intel/national/dashboard | risk_scores | 4 farm score rows | Yes | Yes | The dashboard would feel stronger with more farms and grade variation | Add 6–8 more farm risk rows with LOW, MODERATE, HIGH, and EXTREME grades | High |
| Farm Risk Profile | /climate-intel/farms/:farmId | /api/v1/climate-intel/farms/{farmId}/risk-score | risk_scores, farm_climate_profiles | Not yet rich enough for narrative use | Partial | No | Needs a fuller seasonal and historical profile | Seed 1–2 detailed profiles with component breakdowns | Medium |
| District Risk | /climate-intel/districts/:code | /api/v1/climate-intel/districts/{code}/risk-profile | district_risk_snapshots, climate_alerts | 4 districts | Partial | Partially | Needs richer district heat and supporting alerts | Seed 2–3 more district nodes with open alerts and farm counts | Medium |
| National Risk | /climate-intel | /api/v1/climate-intel/national/dashboard | district_risk_snapshots, risk_scores | 4 district snapshots | Yes | Yes | More district diversity is still needed | Expand to 6–7 districts and 3+ grade categories | High |
| Alerts | /climate-intel/alerts | /api/v1/climate-intel/alerts | climate_alerts | 4 | Yes | Yes | A richer alert history is necessary for a true operations story | Add 2–3 more alerts with status transitions | Medium |
| Reports | /insurance/reports and /settlements/reports | /api/v1/insurance/reports/{name}, /api/v1/settlements/reports/{name} | claims, settlements, insurance_policies | Structural coverage exists | Partial | Partially | Charts and report rows should be populated with multiple categories | Seed one by-status and one by-crop report with varied values | Medium |
| User Management | /users | /api/v1/users | users, user_roles | 1 | Yes | Partially | The page is correct but too sparse for a stakeholder demo | Add 2–3 additional users with different roles and one disabled account | Medium |
| Audit Logs | /users/:id/audit | /api/v1/users/{id}/audit | audit_logs | Not currently rich | No | No | The audit trail is not yet a visible story element | Seed 5–10 audit rows for user and policy actions | Medium |

## Important observations for demo value

1. The current baseline is already strong for the executive, farmers, farms, policies, claims, settlements, GIS, and climate-intelligence story.
2. The biggest gap is workflow depth: tasks, task detail, and decisions are not yet populated enough to make the operations narrative convincing.
3. The climate platform is structurally present, but the datasets and observation pages need richer content to avoid feeling empty.
4. The existing data is synthetic demo data and should remain clearly labeled as such in the documentation and in any future system messaging.

## Recommended demo posture

- Keep the current seeded portfolio as the default presentation baseline.
- Add the missing workflow and drill-down data first if the goal is a judge-facing demo.
- Preserve the current architecture and route model; use the existing backend/domain/persistence services for any future seed expansion.
