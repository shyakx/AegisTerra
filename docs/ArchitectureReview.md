# AegisTerra Architecture Review

**Role:** Chief Software Architect assessment  
**Scope:** Full repository under `AegisTerra/`  
**Baseline version:** 0.1.0  
**Review date:** 2026-08-05  
**Constraint honored:** Analysis and documentation only — no application code was modified.

---

## Executive Summary

AegisTerra is positioned in documentation as a **government-grade national agricultural insurance platform** (GIS-aware, auditable, JWT/RBAC, Clean Architecture, PostgreSQL/PostGIS). The **codebase does not yet implement that target architecture**.

What exists today is a **coherent early scaffold**:

| Layer | Reality |
|-------|---------|
| Backend | Spring Boot 3.3.3 mock API: controllers return hardcoded/echo data; HTTP Basic + fake JWT strings; no JPA, no migrations, no application services |
| Frontend | React/Vite/Tailwind shell with login call and local-state CRUD prototypes; Redux/React Query/i18n/MapLibre largely unused |
| Docs | Strong product vision and three foundational ADRs; ahead of implementation |
| Gap | **Documentation describes a production platform; code is a Phase-0 / Phase-1 prototype** |

**Verdict:** Suitable as an intentional foundation for Phase 1 engineering. **Not** production-ready, **not** yet Clean Architecture in practice, and **not** yet government-grade from a security/compliance/ops perspective.

Primary risks if feature work starts before foundation work: security debt (hardcoded credentials, mock tokens, localStorage tokens without route guards), architecture theater (layers named but empty), and UI–API divergence (frontend mocks vs backend mocks that do not share a real contract).

---

## Current Project Structure

```
GRACE PROJECT/
├── .gitignore
├── AegisTerra_SRS_Business_Architecture V2.docx
└── AegisTerra/
    ├── backend/                 # Spring Boot API
    │   ├── pom.xml
    │   └── src/main/java/com/aegisterra/platform/
    │       ├── AegisTerraApplication.java
    │       ├── api/             # HealthController
    │       ├── common/          # SecurityConfiguration
    │       ├── domain/          # BaseEntity, Farmer, Farm, Policy, Claim (unused by controllers)
    │       └── presentation/    # Controllers + DTOs (mock responses)
    ├── frontend/                # React + Vite SPA
    │   └── src/
    │       ├── api/auth.ts      # Only real API client
    │       ├── layouts/         # AppLayout
    │       ├── pages/           # 10 pages (prototypes + stubs)
    │       ├── routes/          # createBrowserRouter
    │       ├── store/           # Empty Redux store
    │       ├── main.tsx
    │       └── styles.css
    └── docs/                    # Architecture baselines + ADRs
```

### Backend structure (observed)

- **Packages present:** `api`, `common`, `domain`, `presentation`
- **Packages documented but missing:** `application`, `infrastructure`, `security`, `audit`, `integration`, `config`
- **Persistence:** none (no datasource, JPA, Flyway/Liquibase)
- **API surface:** `/api/v1/{health,auth,users,farmers,farms,policies,claims,inspections,weather-alerts}` — list + create only; mock data
- **Security:** in-memory `admin` / `Admin@1234`, HTTP Basic for protected routes; login returns `"mock-access-token"` strings that are **not** validated by the filter chain
- **Tests:** ~9 `@SpringBootTest` + MockMvc smoke tests focused on auth gating

### Frontend structure (observed)

- **16 source files** under `src/`
- **Routing:** `/login` + layout children; **no route guards**
- **State:** Redux Provider with empty reducer; React Query Provider with zero hooks
- **Pages:** Login partially wired; CRUD pages use `useState` seed data; GIS/Settings are stubs
- **Dependencies declared but unused in source:** `react-i18next`, `maplibre-gl`, `recharts`, `react-hook-form`, `zod`, `@tanstack/react-table`, most of React Query/Redux
- **Auth:** tokens stored in `localStorage`; never attached to subsequent requests

### Documentation structure

Strong baseline set: Solution/Business/Security/Database/API/Domain/CodingStandards, CHANGELOG, ADR-001..003. Docs claim capabilities (PostGIS, Liquibase, JWT rotation, RBAC, audit) that **code does not implement**.

### Naming and conventions

| Area | Assessment |
|------|------------|
| Package root `com.aegisterra.platform` | Consistent |
| REST paths `/api/v1/...` kebab-case | Good |
| DTO naming `Create*Request` / `*Response` | Good |
| Layer packages vs actual layers | Misleading — names imply Clean Architecture without application/infra |
| Java version | README says 21; `pom.xml` uses **17** |
| Status fields | Stringly typed (`"ACTIVE"`, `"SUBMITTED"`) instead of enums |

### Dependency and build management

| Stack | Tooling | Notes |
|-------|---------|-------|
| Backend | Maven, Spring Boot parent 3.3.3 | Web, Security, Validation (unused), Actuator, springdoc OpenAPI |
| Frontend | npm, Vite 5, TypeScript strict | No ESLint/Prettier/Vitest scripts |
| DB | Declared in docs only | Not in `pom.xml` |
| CI/CD | Not present | No pipelines observed |

---

## Strengths

1. **Clear product vision** — Business and solution docs define national agri-insurance scope, actors, and capabilities coherently.
2. **Foundational ADRs exist** — PostGIS, JWT+refresh, Clean Architecture are recorded early (correct timing).
3. **Consistent branding and API versioning** — `/api/v1`, AegisTerra naming, government-oriented UI shell.
4. **Sensible technology choices (as targets)** — Spring Boot + React + PostgreSQL/PostGIS are appropriate for this domain.
5. **Frontend visual shell** — Tailwind tokens, sidebar layout, and page scaffolding give a usable UX skeleton for iteration.
6. **Backend test habit started** — Controller smoke tests assert unauthenticated → forbidden and authenticated list/create paths.
7. **DTO boundary begun** — Presentation DTOs are separate from domain types (even though domain is unused).
8. **Repository hygiene** — Duplicate root `frontend/` was removed; `.gitignore` covers `node_modules`, `dist`, `target`.

---

## Weaknesses

### 1. Clean Architecture is aspirational, not implemented

**Why it matters:** Government platforms must isolate business rules from frameworks so policy changes, audits, and replacements (IdP, GIS engines) do not rewrite the core. Controllers that return hardcoded maps with no use-case services make domain rules untestable and invisible.

### 2. Auth model is inconsistent and unsafe for production

Login returns mock JWTs; protected APIs require HTTP Basic; `/auth/me` always 401; credentials hardcoded in security config, auth controller, frontend defaults, and tests.

**Why it matters:** Dual auth stories confuse clients and create a false sense of JWT readiness. Hardcoded passwords in source are a critical defect for any shared or deployed environment.

### 3. No persistence layer

No database, repositories, migrations, or transaction boundaries.

**Why it matters:** Insurance data (farmers, policies, claims, spatial boundaries) requires durability, referential integrity, audit trails, and spatial indexes — none of which exist yet.

### 4. Domain model is anemic and disconnected

`Farmer`/`Farm`/`Policy`/`Claim` POJOs are unused; Inspection and WeatherAlert APIs have no domain types; no enums, aggregates, or invariants.

**Why it matters:** Without an explicit domain, validation and lifecycle rules (claim submission → inspection → payout) will leak into controllers and UI, producing inconsistency and fraud risk.

### 5. No input validation despite validation starter

Zero `@Valid` / Bean Validation annotations; frontend has `zod` unused.

**Why it matters:** Unvalidated insurance payloads accept garbage IDs, amounts, and PII — unacceptable for government systems.

### 6. Frontend–backend contract is fictional

UI pages mutate local state; backend returns mock lists; only login hits the network. No shared OpenAPI-driven client, no env-based API base URL, no Vite proxy.

**Why it matters:** Teams will build divergent shapes; integration will be a rewrite, not a wiring exercise.

### 7. Dependency theater on the frontend

Heavy libraries installed but unused (maps, charts, tables, forms, i18n).

**Why it matters:** Inflates install/build surface, obscures real maturity, and invites inconsistent patterns when features start.

### 8. Missing enterprise cross-cutting concerns

No `@ControllerAdvice`, structured logging/correlation IDs, rate limiting, CORS policy, RBAC enforcement beyond “authenticated”, audit logging, secrets management, profiles (dev/test/prod), or CI.

**Why it matters:** These are non-negotiable for government-grade operability and accountability.

### 9. Documentation–code drift

CHANGELOG claims auth contract and protected shell; CodingStandards forbid mock-only logic; code is mock-heavy. Java 21 documented vs Java 17 in POM.

**Why it matters:** Drift destroys trust in architecture docs as governance artifacts.

### 10. Limited UI architecture

No shared component library, no protected routes, no mobile nav, duplicate form patterns, GIS stub despite `maplibre-gl`.

**Why it matters:** Scale of screens (farmers, farms, policies, claims, GIS) will produce copy-paste UI and accessibility/security gaps.

---

## Risks

| ID | Risk | Severity | Likelihood | Impact |
|----|------|----------|------------|--------|
| R1 | Shipping with hardcoded admin credentials | Critical | High if rushed | Full system compromise |
| R2 | Treating mock JWT as “done” authentication | Critical | High | Unauthorized access / false compliance |
| R3 | Building features on controller mocks without persistence | High | High | Throwaway work, data loss, no audit trail |
| R4 | XSS exfiltration of tokens in `localStorage` | High | Medium | Session hijack of privileged users |
| R5 | Doc-driven false readiness for stakeholders | High | High | Misaligned budget, failed audits |
| R6 | Spatial requirements deferred until late | High | Medium | Costly schema rework; PostGIS ADR ignored |
| R7 | Dual state stacks without ownership rules | Medium | High | Bugs, inconsistent cache/source of truth |
| R8 | No migration strategy when DB is introduced | Medium | High | Schema chaos, irreversible prod changes |
| R9 | PII (national IDs, phones) handled without classification | High | Medium | Legal/compliance exposure |
| R10 | No observability / DR story | Medium | Medium | Unoperable national service |

---

## Recommended Improvements

1. **Freeze feature expansion** until authentication, persistence, and Clean Architecture skeletons are real.
2. **Implement true JWT** (access + refresh rotation/revocation) and remove HTTP Basic for API clients; align frontend Bearer usage and route guards.
3. **Introduce PostgreSQL + PostGIS + Liquibase** with UUID PKs, audit columns, soft delete — per DatabaseDesign and ADR-001.
4. **Add application layer** (use cases/services) and infrastructure repositories; stop returning mocks from controllers.
5. **Wire Bean Validation and Zod** at API and form boundaries.
6. **Establish OpenAPI as contract** — annotate controllers; generate or hand-maintain frontend API modules.
7. **Define frontend architecture rules:** React Query for server state; Redux only for cross-cutting UI/session if needed; feature folders; shared UI primitives.
8. **Externalize secrets**; remove default credentials from UI; use env profiles.
9. **Add CI:** compile, unit/slice tests, lint, dependency audit.
10. **Expand ADRs** for React, Spring Boot, modular monolith, IdP, tenancy, and secrets (see `docs/ADR/`).
11. **Reconcile docs with code** each milestone — update CHANGELOG honestly (scaffold vs delivered).
12. **Threat model + data classification** before collecting real farmer PII.

---

## Refactoring Opportunities

*(Do not execute until approved — listed for planning.)*

| Opportunity | Rationale |
|-------------|-----------|
| Split `presentation` controllers into thin adapters calling application use cases | Enforce Clean Architecture (ADR-003) |
| Move `SecurityConfiguration` from `common` to `security`/`config` | Clear ownership |
| Merge or relocate `api.HealthController` under presentation or actuator-only | Consistent package story |
| Delete or activate unused domain until mapped from DTOs | Avoid dead code lying about maturity |
| Collapse unused frontend dependencies until first real consumer | Reduce noise (KISS) |
| Extract shared form/list page pattern into UI primitives | DRY across Users/Farmers/Policies/Claims/Farms |
| Replace string statuses with domain enums | Type safety, DDD alignment |
| Resolve `Claim.status` shadowing `BaseEntity.status` | Avoid ambiguous audit vs claim workflow status |
| Align Java toolchain to 21 (docs) or update docs to 17 | Single source of truth |
| Add Vite proxy or `VITE_API_URL` | Enable real FE↔BE integration in dev |

---

## Missing Enterprise Features

Relative to documented target and government-grade expectations:

**Security & identity**

- Real JWT issuance/validation, refresh rotation, revocation store
- RBAC/permissions enforced on methods and UI routes
- Account lockout, rate limiting, password policy
- Secrets management (Vault/env), HTTPS headers, CORS policy
- Prefer httpOnly cookie BFF or hardened Bearer strategy over raw `localStorage`

**Data & domain**

- PostgreSQL/PostGIS schema and Liquibase changelogs
- Repositories, transactions, optimistic locking
- Full entity catalog (crops, seasons, premiums, payouts, partners, farm boundaries, audit_logs, history tables)
- Domain services / aggregates for policy and claim lifecycles

**API & integration**

- Pagination, filtering, sorting
- Global exception model / Problem Details
- OpenAPI completeness
- Weather/satellite/payment/IdP integration ports

**Frontend**

- Protected routes, auth session model
- Feature modules, design system
- GIS with MapLibre + farm boundaries
- i18n (Kinyarwanda/English/French likely for national deployment)
- Charts/reporting, accessible forms

**Operations**

- Environments, CI/CD, observability (metrics, traces, structured logs)
- Backup/DR, RPO/RTO, SIEM hooks
- Compliance mapping (local data protection, audit evidence)

**Quality**

- Unit tests for domain; slice tests; Testcontainers; frontend tests; security tests

---

## Priority Improvements

Ordered by dependency and risk reduction (no timelines):

### P0 — Stop the bleeding (security & honesty)

1. Remove hardcoded production-like credentials from runtime paths; document demo-only secrets.
2. Decide and document the **single** auth mechanism (JWT per ADR-002); remove conflicting Basic-vs-Bearer behavior for SPA clients.
3. Add frontend route guards and stop treating login as sufficient app protection.
4. Mark all mock endpoints explicitly in OpenAPI/docs until replaced.

### P1 — Platform foundation

5. Database + Liquibase + PostGIS extension.
6. Application + infrastructure packages; first real use case (e.g. Farmer create/list).
7. Bean Validation on all write DTOs.
8. Global exception handler + correlation ID logging.
9. API client module on frontend consuming real endpoints via React Query.

### P2 — Core insurance domains (after P1)

10. Farmer → Farm (with boundary geometry) → Policy → Claim vertical slices.
11. RBAC roles (Admin, Field Officer, Insurer, Finance).
12. Audit log for mutating operations.
13. GIS map read path for farms/boundaries.

### P3 — National platform capabilities

14. Premiums, payouts, notifications, reporting.
15. Weather/risk/satellite integration modules.
16. i18n, accessibility, performance budgets.
17. Ops: CI/CD, monitoring, backup/DR, compliance annex.

---

## Comparison to Engineering Principles

| Principle | Status | Notes |
|-----------|--------|-------|
| Clean Architecture | **Documented / not practiced** | Missing application & infrastructure; domain unused |
| SOLID | **Weak** | Controllers do everything; no DIP via ports |
| DDD | **Named only** | Anemic POJOs; no aggregates/bounded contexts in code |
| Separation of Concerns | **Partial** | DTOs exist; business logic absent so concern is “mock HTTP” |
| DRY | **Poor on FE** | Repeated CRUD page patterns; duplicated credentials |
| KISS | **Mixed** | Backend mocks are simple; FE dependency set is over-complex for current scope |
| Secure coding | **Fail for target grade** | Hardcoded secrets, mock tokens, localStorage, no validation |

---

## Conclusion

AegisTerra has the **right ambition and mostly right technology bets**, with documentation that already sketches a credible national platform. The implementation is an **early scaffold**. The correct next move is not feature velocity — it is closing the gap between ADRs/docs and a secure, persistent, layered codebase.

**Recommendation:** Approve a foundation-first roadmap (see `DevelopmentRoadmap.md`), retire technical debt deliberately (see `TechnicalDebt.md`), and expand the ADR log before large domain feature builds.

**No application code was changed in this review.**
