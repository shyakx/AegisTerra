# AegisTerra Technical Debt Report

**Review date:** 2026-08-06 (updated Phase 9 / 1.0.0-rc.2)  
**Scope:** Current codebase vs documented government-grade target  
**Related:** `ArchitectureFitness.md`, `DevelopmentRoadmap.md`, `CHANGELOG.md`

Debt is classified by **priority** (P0–P3) and **impact** (Critical / High / Medium / Low).

---

## Resolved in Phase 9 (1.0.0-rc.2)

| ID | Resolution |
|----|------------|
| Application→presentation DTO inversion | DTOs moved to `application.contracts` |
| Scaffold weather-alerts / anemic unused domain POJOs | Removed |
| Users FE mock | Live `/api/v1/users` + roles/permissions/audit |
| Stale health/OpenAPI versions | Synchronized to `1.0.0-rc.2` |
| RbacController repository injection | `RbacQueryService` |
| Layer fitness enforcement | `LayerArchitectureFitnessTest` |
| Profile editing gap | `PUT /api/v1/auth/profile` + Settings UI |
| Observability baseline | probes, metrics, correlation logs, slow queries |

Historical scaffold P0/P1 items (TD-001–TD-017 from early reviews) are **superseded** by Identity 0.3.0+, Persistence 0.4.0+, domain phases 4–8, and RC1/RC2. Treat them as closed unless reopened by regression.

---

## Current Debt

### TD-P9A — User admin search scalability

| Field | Value |
|-------|-------|
| Location | `UserAdminService.searchUsers` |
| Priority | **P3** |
| Impact | **Low** (operator IAM) |
| Description | Search/filter still loads all non-deleted users then filters in memory |
| Recommended fix | JPA Specification / native query with DB pagination when operator count grows |

### TD-P6D — Communication platform follow-ups

| Field | Value |
|-------|-------|
| Location | NotificationDispatcher, channel stubs |
| Priority | **P2** |
| Impact | **Medium** |
| Description | EMAIL/SMS/PUSH/WEBHOOK are logging stubs; no outbox retry/dead-letter worker; no admin template CRUD UI |
| Recommended fix | Real providers behind feature flags; outbox worker |

### TD-P6C — Decision engine follow-ups

| Field | Value |
|-------|-------|
| Location | DecisionService, decision_rules |
| Priority | **P2** |
| Impact | **Medium** |
| Description | No admin CRUD UI for types/rules; escalation scheduler deferred |
| Recommended fix | Decision admin APIs; SLA stage |

### TD-P6B — Task engine follow-ups

| Field | Value |
|-------|-------|
| Location | TaskService, AssignmentStrategy |
| Priority | **P2** |
| Impact | **Medium** |
| Description | DEPARTMENT/REGION strategies stubbed; attachment upload metadata-only |
| Recommended fix | Org/geo pools + real file storage |

### TD-P6A — Workflow foundation follow-ups

| Field | Value |
|-------|-------|
| Location | WorkflowRuntime |
| Priority | **P2** |
| Impact | **Medium** |
| Description | No discard-draft API; terminal outcome inferred from step names |
| Recommended fix | Explicit terminal outcome; SubjectAccessPolicy port |

### TD-P5 — Insurance core follow-ups

| Field | Value |
|-------|-------|
| Location | PolicyDocumentService, PremiumPricingEngine |
| Priority | **P2** |
| Impact | **Medium** |
| Description | Text templates; operator mark-paid without PSP |
| Recommended fix | PDF/PKI + payment gateway |

### TD-P4 — Agricultural spatial follow-ups

| Field | Value |
|-------|-------|
| Location | BoundaryService |
| Priority | **P2** |
| Impact | **Medium** |
| Description | Overlap detection and plot containment deferred |
| Recommended fix | ST_Intersects neighbor checks |

### TD-P7 — Settlement provider stubs

| Field | Value |
|-------|-------|
| Location | PaymentProvider SPI |
| Priority | **P2** |
| Impact | **Medium** |
| Description | Only ManualSettlementProvider operational |
| Recommended fix | Future Advanced Finance phase |

### TD-P8 — Climate provider stubs

| Field | Value |
|-------|-------|
| Location | ClimateDataProvider SPI |
| Priority | **P2** |
| Impact | **Medium** |
| Description | External meteo/EO HTTP providers remain stubs |
| Recommended fix | Provider adapters behind feature flags |

### TD-018 — Mobile navigation polish

| Field | Value |
|-------|-------|
| Location | `AppLayout.tsx` |
| Priority | **P3** |
| Impact | **Medium** |
| Description | Mobile drawer exists; further field UX polish pending |

---

## Summary

Phase 9 closed architectural debt that blocked RC2 promotion (layer inversion, scaffold endpoints, Users mock, version drift). Remaining debt is honest external-integration and scale polish, not foundation theater.