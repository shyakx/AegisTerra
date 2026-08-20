# Task 025 — RBAC Demo Experience Audit

**Status:** COMPLETE  
**Date:** 2026-08-10  
**Release:** 1.0.0-RC2

## 1. Existing RBAC architecture

- **Model:** permission codes on Spring Security authorities (`@PreAuthorize` / `@EnableMethodSecurity`).
- **Source of truth:** Liquibase seeds for `roles`, `permissions`, `role_permissions`; JWT + `/api/v1/auth/me` expose role codes and permission codes.
- **Frontend:** `AuthContext.hasPermission` / `hasRole` / `hasAnyPermission`; `ProtectedRoute`; permission-filtered navigation.
- **No new authorization system** was introduced. No fake role-only screens.

## 2. Existing roles

| Code | Purpose |
|------|---------|
| SYSTEM_ADMIN | Full platform administration |
| INSURANCE_ADMIN | Insurer operations |
| INSURANCE_OFFICER | Policy/claims operations |
| FI_OFFICER | Settlements / payouts |
| GOVERNMENT_ANALYST | National read / climate |
| AGGREGATOR | Farmer/farm onboarding |
| FARMER | Self-service (limited until subject isolation) |
| AUDITOR | Broad read-only |
| SUPPORT | Limited assistance |

## 3. Existing permissions (catalog)

IAM: `users:*`, `roles:read`, `permissions:read`, `auth:admin`  
Agriculture: `farmers:*`, `farms:*`  
Insurance: `policies:read|write|approve`  
Claims: `claims:read|write|assess|decide|admin`  
Workflow: `workflows:*`, `tasks:*`, `decisions:*`, `notifications:*`  
Finance: `settlements:*`, `ledger:read`, `reports:settlements`  
Climate: `climate:*`, `reports:climate`, `climate-intel:*`, `alerts:climate`, `reports:climate-intel`

Unused in `@PreAuthorize` (seeded but not wired on controllers): `auth:admin`, `claims:decide`, `claims:admin`, `settlements:approve`, `settlements:admin`. Settlement **finance UI route** still gates on `settlements:approve`.

## 4. Changes made

### Backend
- `026-demo-rbac-role-alignment.yaml` — persona alignment using **existing** permission codes:
  - **FARMER:** soft-revoke national `farmers/farms/policies/claims:read` (unsafe without subject isolation); keep `notifications:read|write`.
  - **INSURANCE_OFFICER:** grant `policies:write` (issuance).
  - Multiple roles: grant `notifications:write` where inbox exists.
  - **SUPPORT:** grant `tasks:read`.
- `DemoRoleRbacIT.java` — focused 401/403/200 matrix for demo personas.

### Frontend
- `navigation/buildNavigation.ts` — permission-filtered nav + role-aware group/header labels.
- Action gates on agriculture create/edit UIs (`farmers:write` / `farms:write`).
- Claim / settlement task & assess links gated.
- Dashboard executive fetch + deep links gated; FARMER gets honest limited portal home.
- Settings smoke links filtered by permission.
- Notification preferences write gated.
- `ProtectedRoute` supports `anyOf`.

### Docs
- This report; demo script already lists all role accounts.

## 5. Role-by-role access matrix (post-026)

| Role | Farmers | Policies | Claims | Settlements | Users | Climate intel | Executive |
|------|---------|----------|--------|-------------|-------|---------------|-----------|
| SYSTEM_ADMIN | 200 | 200 | 200 | 200 | 200 | 200 | 200 |
| INSURANCE_OFFICER | 200 | 200 | 200 | **403** | **403** | **403** | 200 |
| FI_OFFICER | 200 | 200 | 200 (read) | 200 | **403** | **403** | 200 |
| GOVERNMENT_ANALYST | 200 | 200 | 200 | 200 (read) | **403** | 200 | 200 |
| FARMER | **403** | **403** | **403** | **403** | **403** | **403** | **403** |
| AUDITOR | 200 | 200 | 200 | 200 | 200 (read) | 200 | 200 |

## 6. Backend authorization verification

- Business controllers already used `@PreAuthorize`; no broad permitAll expansion.
- Live verification (2026-08-10) confirmed the matrix above.
- `DemoRoleRbacIT` passed via `mvnw -Dtest=DemoRoleRbacIT test`.

## 7. Frontend navigation verification

| Role | Visible nav intent |
|------|--------------------|
| SYSTEM_ADMIN | Full command / agri / insurance / settlement / climate / admin |
| INSURANCE_OFFICER | Registry read + insurance/claims + tasks; no finance/users |
| FI_OFFICER | Financial operations + supporting reads + tasks |
| GOVERNMENT_ANALYST | National overview + read modules + climate |
| FARMER | Overview + notifications + account only |
| AUDITOR | Broad read modules + users read; mutation controls hidden |
| SUPPORT | Support desk + registry lookup + users read + tasks |

## 8. Route protection verification

- Existing `ProtectedRoute(permission)` retained for major modules.
- Typing URLs without permission still yields **Access denied** in-shell.
- Backend remains authoritative (403).

## 9. Data-isolation findings

**Gap (documented, not faked in React):**
- `farmers.user_id` exists but is unused for list/get filtering.
- Farmer/farm/policy/claim list APIs are **platform-wide** for any holder of `*:read`.
- Safer demo behavior: FARMER no longer receives those read grants.
- Own farm / own policy / own claim self-service **requires future subject-scoped APIs** — remaining gap.

Draft ownership (`createdByUserId`) already scopes some draft endpoints; notifications are user-owned.

## 10. Tests executed

| Suite | Result |
|-------|--------|
| `backend`: `.\mvnw.cmd -Dtest=DemoRoleRbacIT test` | PASS |
| `frontend`: `npm run build` (`tsc -b && vite build`) | PASS |

No frontend unit-test runner is configured in the project; build/typecheck is the frontend gate used.

## 11. Remaining RBAC gaps

1. Farmer subject isolation (link user ↔ farmer; scope list/get APIs).
2. Permission/API drift: `claims:decide`, `claims:admin`, `settlements:approve`, `settlements:admin`, `auth:admin` unused on controllers.
3. Workflow assignees still often seeded as `SYSTEM_ADMIN` role pools (ops demo still admin-centric for inbox claim).
4. INSURANCE_ADMIN still lacks climate/settlement reads (persona optional; not invented).
5. Global 401 → forced logout UX still soft.
6. No dedicated frontend RBAC component tests (no Vitest/Jest harness).

## 12. Competition demo recommendation

**Ready for multi-role demo** with these talk tracks:

1. Start as **admin** — full national command center.
2. Switch to **insurance.officer** — claims/policies workstation; settlements/users blocked.
3. Switch to **fi.officer** — settlements/ledger/finance review; no user admin.
4. Switch to **gov.analyst** — climate + national read posture; no mutations/admin.
5. Switch to **farmer.demo** — show deliberate limited portal + explain isolation gap honestly if asked.
6. Switch to **auditor** — read-only breadth without approve/process controls.

Passwords unchanged: `admin` / `Admin@1234!Aa`; other roles / `Demo@1234!Aa`.

---

## Files changed (summary)

- `backend/.../026-demo-rbac-role-alignment.yaml`
- `backend/.../db.changelog-master.yaml`
- `backend/.../DemoRoleRbacIT.java`
- `frontend/src/navigation/buildNavigation.ts` (new)
- `frontend/src/layouts/AppLayout.tsx`
- `frontend/src/auth/ProtectedRoute.tsx`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/FarmersPage.tsx`, `HouseholdsPage.tsx`, `CropsPage.tsx`, `SeasonsPage.tsx`, `FarmPlotsPage.tsx`, `FarmBoundaryPage.tsx`, `CropHistoryPage.tsx`, `FarmDetailsPage.tsx`
- `frontend/src/pages/ClaimDetailsPage.tsx`, `SettlementDetailsPage.tsx`, `SettlementsPage.tsx`
- `frontend/src/pages/SettingsPage.tsx`, `NotificationPreferencesPage.tsx`
- `docs/Task025-RBAC-Demo-Audit.md` (this file)
