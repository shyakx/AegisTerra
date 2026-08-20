# AegisTerra Development Roadmap

**Purpose:** Dependency-ordered implementation phases for evolving the scaffold into a government-grade agricultural insurance platform.  
**Rule:** Later phases depend on earlier phases. Do not start a phase until its dependencies are satisfied.  
**Timelines:** Intentionally omitted.

Companion documents: `ArchitectureReview.md`, `TechnicalDebt.md`, `ADR/`.

---

## Phase 0 — Governance Alignment

**Goal:** Make documentation and engineering decisions the single source of truth before code churn.

**Depends on:** Architecture review acceptance.

**Deliverables:**

- Accepted Architecture Review findings
- Expanded ADR set (React, Spring Boot, modular monolith, JWT/PostGIS/Clean Architecture refined)
- Explicit “scaffold vs production” status in CHANGELOG
- Toolchain truth: Java version (17 vs 21), Node version
- Definition of Done for foundation phases (security, persistence, layering)

**Exit criteria:** Stakeholders agree features wait until Phase 2+ foundations exist.

---

## Phase 1 — Project Foundation

**Goal:** Executable, honest modular monolith skeleton with real cross-cutting infrastructure.

**Depends on:** Phase 0.

**Backend:**

- Enforce package layout: `domain` → `application` → `infrastructure` → `presentation`
- Configuration profiles (`local`, `test`, `prod` placeholders)
- Global exception handling (Problem Details / standardized errors)
- Structured logging + correlation IDs
- OpenAPI configuration and baseline annotations
- Actuator hardening plan (what is public vs authenticated)
- Remove or quarantine hard-coded credentials behind profile-gated demo config

**Frontend:**

- Env-based API base URL (`VITE_API_URL`) or Vite proxy
- Shared `apiClient` (fetch wrapper) with error normalization
- ESLint + Prettier + TypeScript strict CI scripts
- Decide state ownership: React Query = server state; Redux only if justified (document in ADR or frontend standards)
- Remove or defer unused dependencies until first consumer

**Repo / ops baseline:**

- CI: backend test + frontend typecheck/build
- Secrets never committed; `.env.example` only

**Exit criteria:** Both apps build in CI; layer packages exist; no silent mock-as-production ambiguity in docs.

**Status (2026-08-05):** Implemented in codebase version **0.2.0**. See `CHANGELOG.md`.

---

## Phase 2 — Authentication & Authorization

**Goal:** Implement ADR-002 for real.

**Depends on:** Phase 1 (config, exception handling, API client).

**Backend:**

- User/role/permission persistence (minimal IAM tables)
- Password hashing (already BCrypt-capable) with stored users — not in-memory only
- JWT access token issuance and signature validation filter
- Refresh tokens with **rotation and revocation**
- Replace HTTP Basic as the SPA auth mechanism
- `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me` behaving correctly
- Method/route RBAC scaffolding (`@PreAuthorize` or equivalent)
- Login audit events (success/failure)

**Frontend:**

- Login without default production passwords in committed UI (demo mode flag if needed)
- Secure session handling strategy (document choice: Bearer+memory vs BFF cookies)
- Protected route guards; redirect unauthenticated users
- Attach access token to API calls; handle 401 → refresh → retry → logout
- Basic role-aware nav (hide admin-only routes)

**Exit criteria:** End-to-end login → authenticated API call → me endpoint; refresh rotation works; Basic auth not required for SPA.

**Status (2026-08-05):** Implemented as Identity & Security Platform in **0.3.0** (see `SecurityArchitecture.md`, ADR-007, `CHANGELOG.md`). H2 used for local/test; PostgreSQL required for prod profile.

---

## Phase 3 — Persistence & Spatial Core

**Goal:** Introduce PostgreSQL + PostGIS per ADR-001; migrations per DatabaseDesign baseline.

**Depends on:** Phase 1; IAM tables from Phase 2 may land here or just before.

**Deliverables:**

- Datasource configuration by profile
- Liquibase (or Flyway — confirm via ADR if not already chosen in DatabaseDesign) changelogs
- Enable PostGIS extension
- Core schema: audit columns, UUID PKs, soft delete conventions
- Administrative geography placeholders (district/sector/cell/village) as needed
- Spring Data repositories behind application ports
- Testcontainers (or equivalent) for repository/integration tests

**Exit criteria:** App starts against real DB; migrations apply cleanly; spatial type usable in at least one table (e.g. farm boundary stub).

**Status (2026-08-05):** Implemented as Enterprise Data Platform in **0.4.0** (see `DataArchitecture.md`, `PostGIS.md`, `CHANGELOG.md`). Local default is PostGIS via Docker Compose; tests use Testcontainers PostGIS. Domain APIs/workflows remain Phase 4+.

---

## Phase 4 — Agricultural Core Domain (Farmers + Farms)

**Goal:** Digital identity of farmers and farms — operational heart of AegisTerra (no insurance yet).

**Depends on:** Phase 2 (authz), Phase 3 (persistence/PostGIS).

**Deliverables:**

- `FarmerDomain.md` design gate
- Household, Farmer, Farm, Boundary, Plot, Crop, Season, CropSeason services + APIs
- Guided registration wizard with draft save/submit
- MapLibre boundary draw/edit + PostGIS validation/area
- Enterprise search, pagination, CSV export
- Agri mutation auditing (old/new JSON)
- Production FE pages (no mock list data)

**Exit criteria:** Registration + CRUD + geometry + search + Swagger + tests green; no Policy Management until approved.

**Status (2026-08-05):** Implemented in codebase version **0.5.0**. See `FarmerDomain.md`, `CHANGELOG.md`, `Phase4Deliverables.md`.

---

## Phase 5 — Insurance Core Domain

**Goal:** Insurance policies as financial contracts — products, configurable premium engine, enforced lifecycle, documents, portfolio reporting. No claims yet.

**Depends on:** Phase 4 Agricultural Core acceptance.

**Deliverables:**

- Design gate: `InsuranceDomain.md` (**must be approved before code**)
- Products, policy types, coverage packages, beneficiaries, documents
- Strategy-based premium pricing engine (no hardcoded formulas)
- Policy state machine + issuance wizard
- Search, audit, reporting APIs + enterprise UI
- Tests: unit, pricing, workflow, state machine, API

**Exit criteria:** Quality gate in `InsuranceDomain.md`; no Claims Management until Phase 5 accepted.

**Status (2026-08-05):** Implemented in codebase version **0.6.0**. See `InsuranceDomain.md`, `CHANGELOG.md`, `Phase5Deliverables.md`. Accepted for progression to Phase 6 design.

---

## Phase 6 — Enterprise Workflow & Business Process Engine

**Goal:** Reusable workflow, task, approval, notification, and timeline platform. Claims become the **first consumer** after the engine quality gate — claims must not implement a private workflow.

**Depends on:** Phase 5 Insurance Core (v0.6.0).

**Deliverables:**

- Design gate: `WorkflowArchitecture.md` + `ADR-008` (**must be approved before code**)
- Workflow definitions (versioned JSON graphs), instances, steps, tasks, transitions
- Assignment, delegation, reassignment, escalation, SLA
- Approval policies: single, sequential, parallel, majority, auto approve/reject
- Notification rules + channel adapters (in-app required; email/SMS/push/webhook SPI)
- Audit timeline API + reusable frontend timeline + task inbox
- Seeded definitions including `CLAIM_STANDARD` readiness
- **Then:** Claims Management as consumer (separate claims design gate / stage 6F)

**Exit criteria:** Quality gate in `WorkflowArchitecture.md`; no Claims domain workflow duplication; no Settlement until Claims consumer path is accepted.

**Status (2026-08-05):** Design approved. **Stages 6A–6D (0.7.0–0.10.0)** delivered. **Claims consumer (Phase 6F / 0.11.0)** implemented on sequential `CLAIM_STANDARD` / `CLAIM_FAST_TRACK`. Awaiting Workflow Stage **6E** (SLA/escalations & approval policies) before production dual-control adjudication.

**Roadmap note:** Supersedes the earlier “Phase 6 = Claims & Inspections only” framing. Inspections become task types / claim steps on the shared engine.

---

## Phase 7 — Enterprise Financial Settlement Platform

**Goal:** Reusable money-movement platform (Claims, subsidies, refunds, incentives, disaster relief). Claims never call PSPs.

**Depends on:** Phase 6F Claims (`ClaimApprovedForPayment`); Workflow/Task/Decision/Event platforms (6A–6D).

**Design gate:** `docs/SettlementDomain.md` (**approved** — Option A Claims closure).  
**Governance:** `docs/ArchitectureGovernance.md`.

**Deliverables (after approval):** Settlement aggregate, ledger, PaymentProvider SPI (Manual only), `SETTLEMENT_STANDARD` workflow, APIs, finance UI, reports, tests → **0.12.0**.

**Exit criteria:** Quality gate in `SettlementDomain.md`; approved claim can complete settlement via Manual provider with immutable ledger entries; no Claims→payment coupling.

**Status (2026-08-06):** **Implemented in 0.12.0.**

---

## Phase 8A — Climate Data Platform

**Goal:** Collect, normalize, validate, store, and expose climate data only. No AI, Claims, Insurance, or Risk calculations.

**Depends on:** Phase 2 IAM · Phase 3 PostGIS · Phase 4 Agriculture (optional spatial context).

**Design:** `docs/ClimateDataPlatform.md`  
**Governance:** `docs/ArchitectureGovernance.md`.

**Deliverables:** Providers catalog, stations/observations/satellite/raster/datasets, import jobs, validation + quality, Manual/CSV ingest, stub provider SPI, APIs + climate UI.

**Status (2026-08-06):** **Implemented in 0.14.0** (shipped with 8B).

---

## Phase 8B — Climate Intelligence & Risk Platform

**Goal:** Deterministic environmental intelligence (risk scores, drought/flood/rainfall/vegetation indicators, alerts, profiles) that **consumes** Climate Data Platform only.

**Depends on:** Phase 8A Climate Data Platform **implemented**; Phase 4 farm geometry.

**Design:** `docs/ClimateIntelligencePlatform.md`  
**Governance:** `docs/ArchitectureGovernance.md`.

**Deliverables:** Risk engine, indicators, alerts, farm/district/national views, APIs + UI.

**Non-goals:** ML/neural nets/prediction models; fraud; Claims/Insurance logic; direct external climate providers.

**Status (2026-08-06):** **Implemented in 0.14.0** (shipped with 8A).

> **Roadmap remap:** Former monolithic “Phase 8 Weather/Risk” is split into **8A Climate Data** then **8B Intelligence/Risk**. Former “Phase 7 Weather” remains superseded by Settlement (Phase 7).

---

## Phase 9 — Advanced Finance & Reconciliation (post-Settlement)

**Goal:** Real PSP adapters, statement reconciliation, advanced portfolio reporting.

**Depends on:** Phase 7 Settlement Platform; **not started** as of 1.0.0-rc.2.

**Deliverables:**

- Bank / MoMo / Treasury provider adapters (non-stub)
- Reconciliation jobs against statements
- Export-friendly finance APIs / charts as needed

**Exit criteria:** At least one non-manual provider path in a non-prod environment; reconciliation prototype against fixture statements.

**Status (2026-08-06):** Deferred — RC2 hardening completed without opening this phase.

---

## Phase 9B — National Platform Hardening (Enterprise / RC2)

**Goal:** Government-grade non-functionals and architectural debt retirement before GA.

**Depends on:** Phases 2–8 + RC1 demo candidate.

**Deliverables (RC2 scope completed):**

- Clean Architecture contract placement (`application.contracts`)
- Remove scaffold/fake endpoints and dead packages
- Complete Users administration against live IAM
- Version consistency across build metadata / health / OpenAPI / UI
- Observability baseline (correlation IDs, probes, metrics, slow queries)
- Security header / RBAC / session lifecycle review
- Documentation sync (`TechnicalDebt`, `ArchitectureFitness`, `API`, `CHANGELOG`)

**Still future (post-RC2 / ops pack):**

- Threat model annex + pen-test remediation cycle
- Full i18n / a11y certification pack
- Distributed tracing / log shipping
- Backup/DR evidence pack

**Exit criteria (RC2):** Architecture Health Review P0/P1 debt closed; tests green; promote **1.0.0-RC2**.

**Status (2026-08-06):** **Implemented in 1.0.0-rc.2.**

---

## Phase 10 — Integrations & Scale-Out Options

**Goal:** External systems and evolution path without premature microservices.

**Depends on:** Phase 10 controls for any external PII exchange; stable core domains.

**Deliverables:**

- IdP integration (if replacing local auth) — new ADR
- Payment gateway adapter
- Weather/satellite provider adapters
- Multi-tenancy model clarification (ADR-002 mentioned multi-tenant — decide isolation strategy)
- Asynchronous processing where needed (outbox/messaging) — ADR first
- Only then consider extracting modules to services if scale demands

**Exit criteria:** Integrations behind ports; modular monolith remains default unless ADR supersedes.

---

## Dependency Graph (summary)

```
Phase 0 Governance
    └── Phase 1 Foundation
            └── Phase 2 AuthN/AuthZ
                    └── Phase 3 Persistence + PostGIS
                            ├── Phase 4 Agricultural Core
                            │       └── Phase 5 Insurance Core (policies / premium engine)
                            │               ├── Phase 6 Workflow Engine → Claims (6F)
                            │               │       └── Phase 7 Settlement Platform → Phase 9 Reconciliation/PSPs
                            │               └── Phase 8 Climate / Weather / Risk (can parallelize carefully after 4/5)
                            └── Phase 10 Hardening (overlaps late domains)
                                    └── Phase 11 Integrations
```

**Parallelism note:** Frontend UI polish can track each domain phase, but must not invent permanent local-only data models once Phase 4+ APIs exist.

---

## Anti-Patterns to Avoid

- Building Policies/Claims UI against `useState` mocks after Phase 3 exists
- Adding microservices before modular monolith module boundaries are clean
- Implementing satellite ML before farm geometry and audit exist
- Declaring “JWT done” while HTTP Basic remains the real gate
- Collecting real citizen PII before Phase 10 controls
