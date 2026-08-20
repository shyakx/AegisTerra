# AegisTerra DEMO RBAC Matrix

**Release:** 1.0.0-rc.2  
**Scope:** Competition demo personas — navigation, dashboards, route guards, and backend permission grants.  
**Principle:** Frontend hides modules for UX; **backend `@PreAuthorize` remains authoritative**.

## Demo accounts

| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin@1234!Aa` | SYSTEM_ADMIN |
| `insurance.admin` | `Demo@1234!Aa` | INSURANCE_ADMIN |
| `insurance.officer` | `Demo@1234!Aa` | INSURANCE_OFFICER |
| `fi.officer` | `Demo@1234!Aa` | FI_OFFICER |
| `gov.analyst` | `Demo@1234!Aa` | GOVERNMENT_ANALYST |
| `aggregator` | `Demo@1234!Aa` | AGGREGATOR |
| `farmer.demo` | `Demo@1234!Aa` | FARMER (scoped to `FRM-RC1-001`) |
| `auditor` | `Demo@1234!Aa` | AUDITOR |
| `support` | `Demo@1234!Aa` | SUPPORT |

All roles land on `/` (persona dashboard). Navigation is role-catalog + permission filtered.

## Role matrix

| Role | Dashboard | Visible Modules | Read Actions | Write / Approval Actions | Restricted Areas |
|------|-----------|-----------------|--------------|--------------------------|------------------|
| **SYSTEM_ADMIN** | Operations Command Center | Full: agri, insurance, claims, settlements, ledger, climate, climate intel, users, tasks, notifications | All platform reads | Admin + operational mutations permitted by grants | None (full portal) |
| **INSURANCE_ADMIN** | Insurance portfolio | Overview, tasks, notifications, farmers/farms/GIS, policies/products/calculator/reports, claims, climate reads | Registry, policies, claims, climate, tasks | Policy write/approve; claims write/assess/decide; tasks/decisions | Users/admin; settlements; ledger; settlement finance |
| **INSURANCE_OFFICER** | Insurance workstation | Overview, my tasks, notifications, policies, claims, reports, farmers/farms | Registry + insurance operational reads | Policy write (issue); claims write/assess; tasks/decisions act | Users; settlements; ledger; climate admin; products admin not emphasized |
| **FI_OFFICER** | Settlement desk | Overview, tasks, notifications, settlements, finance review, reports, ledger, payment providers; claims/policies supporting reads | Settlements, ledger, claims/policies supporting | Settlement process/approve (where granted); tasks/decisions | Users; insurance product admin; climate intel; claim write |
| **GOVERNMENT_ANALYST** | National analytics / climate | Executive, farmers/farms/GIS, policies/claims/reports, climate + climate intel, settlements/ledger reports (read) | National oversight reads | Notifications mark-read only | Users write; claim approve/assess; settlement process; policy mutate |
| **AGGREGATOR** | Farmer portfolio | Overview, tasks, notifications, households/farmers/farms/GIS, policies (context) | Farmers/farms/policies/tasks | Farmer/farm write (onboarding) | Users; claims mutate; settlements; climate admin; policy issue |
| **FARMER** | Personal farmer home | Home, notifications, my profile, my farms, my policies, my claims | Subject-scoped farmers/farms/policies/claims + own notifications | Claim file (`claims:write`); notification preferences | Command center modules; GIS/crops/seasons/households; insurance products/calc/reports; users; settlements; ledger; tasks |
| **AUDITOR** | Oversight / audit | Broad read: agri, insurance, claims, settlements, ledger, climate, users (read), tasks | Broad operational reads | Notifications mark-read only | Claim/settlement/policy mutations; user write |
| **SUPPORT** | Support desk | Overview, tasks, notifications, users lookup, farmers/farms, policies, claims | Lookup reads | Notifications write (inbox); no financial approvals | Settlements process; claim assess/approve; user write; climate admin; ledger |

## Route guarding (frontend)

| Path pattern | Guard |
|--------------|-------|
| `/users` | `users:read` |
| `/farmers`, `/farms`, … | `farmers:read` / `farms:read` |
| `/households`, `/crops`, `/seasons`, `/gis` | permission + **deny `FARMER`** |
| `/policies`, `/claims`, … | `policies:read` / `claims:read` (+ write/assess nested) |
| `/insurance/products\|calculator\|reports` | `policies:read` + **deny `FARMER`** |
| `/settlements` | `settlements:read` |
| `/settlements/finance` | `settlements:approve` |
| `/ledger` | `ledger:read` |
| `/climate` | `climate:read` |
| `/climate-intel` | `climate-intel:read` |
| `/tasks` | `tasks:read` |

Manual URL entry without permission shows **Access denied**. Backend still returns **401/403**.

## Backend permission corrections (Liquibase)

| ChangeSet | Correction |
|-----------|------------|
| `025-demo-seed-role-permissions` | Seeded persona-usable grants for all non-admin roles (existing permission codes only). |
| `026-demo-rbac-role-alignment` | Soft-revoked unsafe FARMER national reads; officer `policies:write`; `notifications:write` for inbox roles; SUPPORT `tasks:read`. |
| `027-demo-role-page-access` | Restored **FARMER** subject-scoped reads + `claims:write`; SUPPORT policies/claims; AGGREGATOR policies/tasks; INSURANCE_ADMIN climate reads. Undelete-before-insert for `uk_role_permissions`. |

## Farmer subject scoping

- Demo binder maps `farmer.demo` → farmer `FRM-RC1-001` (Jean Niyonzima).
- List/get APIs for farmers/farms/policies/claims filter to that subject for FARMER.
- Frontend farmer dashboard and soft page titles use the same scoped APIs — **no hardcoded fake records**.

## Known remaining gaps

1. Some seeded permissions unused on controllers (`claims:decide`, `claims:admin`, `settlements:approve`, `auth:admin`) — finance UI still gates on `settlements:approve`.
2. Workflow assignee pools often still admin-centric in seed data.
3. Claim create should ideally validate policy ownership before insert (post-create assert is weaker).
4. No dedicated frontend Vitest RBAC suite (build/typecheck + `DemoRoleRbacIT` are the gates).
