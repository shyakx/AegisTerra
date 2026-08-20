# AegisTerra Architecture Governance

**Version:** 1.1.0  
**Status:** Active  
**Applies to:** All platform phases from 0.12.0 onward (retroactive guidance for 0.7.0–0.11.0)  
**Related:** `DevelopmentRoadmap.md`, `WorkflowArchitecture.md`, `EventArchitecture.md`, `ClaimsDomain.md`, `SettlementDomain.md`, `ClimateDataPlatform.md`, `ClimateIntelligencePlatform.md`, `ArchitectureFitness.md`, ADR-008  

This document is the permanent rulebook for module boundaries, naming, migrations, events, security, and Definition of Done. Domain design docs specify *what* a phase builds; this specifies *how* phases may touch the platform.

> **Phase 7 note (0.12.0):** Settlement Platform delivered under Option A (`ClaimsSettlementListener` only). Intake/closure listeners use `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` so claim adjudication TX is never poisoned by settlement work.

> **Phase 8A note:** Climate Data Platform design is **approved** (`ClimateDataPlatform.md`). No climate implementation until explicitly scheduled.

> **Phase 8B note:** Climate Intelligence design gate is `ClimateIntelligencePlatform.md`. Consumes 8A only — no raw ingest, no external providers, no ML/Claims/Insurance logic.

---

## 1. Allowed dependencies

```text
presentation  → application → domain
                    ↓
              infrastructure (persistence, security, SPI adapters)
                    ↓
              shared (errors, utils)

Business modules (Claims, Settlement, Insurance, Agriculture)
  → MAY depend on: Workflow/Task/Decision/Notification *application ports*, EventBus, Identity audit
  → MUST NOT depend on: each other's persistence entities (use UUID refs + events)
  → MUST NOT call: payment providers, SMTP, SMS, push from domain services

Claims / Subsidies / future money sources
  → MUST NOT call Settlement REST or PaymentProvider directly for happy-path payout
  → MUST publish domain events (e.g. ClaimApprovedForPayment)

Settlement
  → MUST NOT write Claims tables directly
  → MAY emit events that Claims consumes to close lifecycle (thin listener only)
```

**Engines are platforms:** Workflow Runtime, Task Engine, Decision Engine, Communication Engine, EventBus transport adapters are changed only in dedicated engine phases — never as a side effect of a business module PR.

**Config vs code:** Seeding workflow definitions, decision rules, notification templates, and permissions via Liquibase is allowed. Changing engine Java for a business feature is not.

---

## 2. Naming conventions

| Layer | Convention | Example |
|-------|------------|---------|
| Packages | `com.aegisterra.platform.{application\|domain\|infrastructure\|presentation}.{module}` | `application.settlement` |
| Tables | `snake_case`, plural aggregates | `settlements`, `ledger_entries` |
| Permissions | `{resource}:{action}` | `settlements:approve` |
| Workflow codes | `{DOMAIN}_{VARIANT}` | `SETTLEMENT_STANDARD` |
| Events | PascalCase verb phrase | `SettlementCompleted` |
| Numbers | `{PREFIX}-{year}-{8 digits}` | `STL-2026-00001234` |
| Liquibase files | `NNN-{area}-{phase}.yaml` | `017-settlement-phase7.yaml` |
| IT classes | `{Feature}IntegrationTest` | `SettlementPlatformIntegrationTest` |

---

## 3. API versioning policy

- Public HTTP API lives under `/api/v1/...`.
- Breaking changes require `/api/v2` or additive fields with compatibility window — never silent renames.
- Controllers: OpenAPI `@Tag` + `@Operation`; responses use DTOs (records), not entities.
- Errors: existing `ApiError` / problem shape; prefer `400` validation, `401/403` authz, `404` missing, `409` illegal state, `422` business rule.

---

## 4. Database migration policy

1. All schema changes via Liquibase under `db/changelog/changes/`; include from `db.changelog-master.yaml`.
2. Prefer additive changes; avoid destructive drops in the same release as readers still need columns.
3. Soft delete (`deleted` boolean) + audit columns on business tables.
4. Flow-style YAML: quote types with parentheses (`"numeric(19,4)"`); index columns must be `- column: { name: x }` not bare `{ name: x }`.
5. Seeds are idempotent (`WHERE NOT EXISTS`).
6. Never edit an already-shipped changeset that ran in shared environments — add a new changeset.

---

## 5. Event naming & publishing

- Types are stable strings in `DomainEventTypes`.
- Envelope: `PlatformDomainEvent` (`eventId`, `eventType`, `occurredAt`, `actorId`, `subjectType`, `subjectId`, `correlationId`, `payload`).
- Publish via `EventBus` port only.
- Consumers: `@EventListener` / future broker; ignore unknown types; prefer idempotent handling.
- Communication templates keyed by event type code; register new types in handler allow-lists when notifications are required.
- Prefer AFTER_COMMIT semantics for side effects when introducing outbox (debt until then: document same-TX risk).

---

## 6. Error & audit standards

- Domain illegal transitions → `409 CONFLICT` with clear message.
- Business eligibility failures → `422 UNPROCESSABLE_ENTITY`.
- Every material mutation: IAM `AuditAction` + domain history table where applicable + workflow timeline when orchestrated.
- Share `correlationId` across claim/settlement/workflow chain.

---

## 7. Security requirements

- JWT HttpOnly cookies; no tokens in localStorage.
- Method security `@PreAuthorize` on every mutating/read API.
- Least privilege: new permissions per module; map `SYSTEM_ADMIN`, domain officer roles, `AUDITOR` read.
- Secrets only in env / vault — never Liquibase seeds or git.
- Provider credentials behind SPI config (future); Manual provider needs `settlements:disburse`.

---

## 8. Deprecation policy

1. Mark deprecated in OpenAPI + CHANGELOG.
2. Keep ≥1 minor version of dual support for response fields.
3. Remove only in a documented major/minor with migration notes.

---

## 9. Coding standards (platform)

- Java 21 / Spring Boot 3; match existing package patterns.
- Prefer constructor injection; thin controllers.
- Frontend: React + existing `apiClient`, EnterpriseTable, permission-gated routes.
- Do not invent parallel inbox/timeline/decision UIs — compose engine screens.
- Tests: SharedPostgresContainer ITs for critical paths; unit tests for lifecycle/money rules.

---

## 10. Definition of Done (every phase)

A phase is done only when:

1. Design gate doc status → Implemented (or explicit Approved + shipped notes).
2. Liquibase + entities + services + REST + OpenAPI + RBAC.
3. Engine consumption verified (no private BPM/notification/payment tables).
4. Frontend routes behind permissions; responsive enough for ops use.
5. Integration test covers primary happy path end-to-end.
6. CHANGELOG + API.md + Database.md + roadmap updated; semver bumped.
7. Technical debt listed honestly (stubs, 6E, providers, etc.).
8. Final readiness summary delivered to stakeholders (phase-specific checklist).

---

## 11. Phase sequencing reminder

| Semver (indicative) | Phase |
|---------------------|-------|
| 0.11.0 | Claims Management (6F) — done |
| 0.12.0 | **Settlement Platform (Phase 7)** — design gate in `SettlementDomain.md` |
| later | Climate Intelligence / Risk (former roadmap Phase 7) |
| later | Workflow 6E SLA / multi-approver |
| later | Real PSPs + reconciliation |

---

## 12. Exceptions

Exceptions to this governance require:

1. Explicit note in the phase design doc.
2. CHANGELOG “Honest status / debt” entry.
3. Prefer thin listeners and SPI over engine forks.
