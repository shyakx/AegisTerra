# AegisTerra Settlement Domain

**Version:** 1.1.0 (Implemented — Phase 7 / release **0.12.0**)  
**Status:** **Implemented** (Option A Claims closure approved and delivered)  
**Target release:** **0.12.0**  
**Depends on:** Phase 2 IAM · Phase 5 Insurance (beneficiary/policy currency) · Phase 6A–6D Workflow/Task/Decision/Event · Phase 6F Claims (`ClaimApprovedForPayment`)  
**Related:** `ClaimsDomain.md`, `WorkflowArchitecture.md`, `EventArchitecture.md`, `ArchitectureGovernance.md`, ADR-008  

> **Naming note:** Roadmap historically labeled Phase 7 as Weather/Risk and Phase 8 as Payouts. This document **redefines Phase 7 as the Enterprise Financial Settlement Platform**. Climate Intelligence becomes Phase 8+. See roadmap note below.

---

## 1. Domain Vision

The Settlement Platform is the **only** money-movement bounded context in AegisTerra. Every outbound payment — Claims indemnities, government subsidies, premium refunds, partner reimbursements, farmer incentives, disaster relief, and future compensation programs — creates a **Settlement** against a frozen financial snapshot, is orchestrated by the **shared Workflow Engine**, executed through a **PaymentProvider SPI**, and posted to an **immutable internal ledger**.

Settlement must:

1. Be **source-agnostic** (`source_module` + `source_record_id`), not Claims-private.
2. **Never** call PSPs from Claims / Subsidies / other domains — they publish events; Settlement listens.
3. Treat **ledger** as the accounting source of truth (providers are execution adapters only).
4. Reuse Workflow / Task / Decision / Notification engines — **no private BPM, inbox, or mailer**.
5. Freeze beneficiary and amount at creation time (**financial snapshot**).

**Non-goals (v1):**

- Real bank / MoMo / treasury integrations (SPI stubs only; **ManualSettlementProvider** implemented).
- Bank-statement reconciliation jobs (schema hooks only).
- Multi-currency FX engine (store `exchange_rate` on snapshot; default 1.0 same-currency).
- Modifying Workflow Runtime, Task Engine, Decision Engine, Communication Engine, or EventBus transport.
- Bulk mass-payment files / ISO20022 (future).

---

## 2. Hard Constraints

| Constraint | Rule |
|------------|------|
| Engine isolation | **Do not modify** Workflow Runtime, Task Engine, Decision Engine, Communication Engine, EventBus core |
| Claims isolation | Claims **must not** call payment APIs. Claims already emits `ClaimApprovedForPayment` |
| No duplicate infra | No Settlement-private task/approval/notification tables |
| Config seeding OK | Liquibase may seed `SETTLEMENT_*` workflow definitions + templates + permissions |
| Dual-control | Sequential `SETTLEMENT_STANDARD` until engine 6E (parallel/majority/SLA = debt) |

---

## 3. Critical Design Decision — Closing Claims

Claims lifecycle expects `PAYMENT_PENDING → SETTLED → CLOSED` when funds clear. Strict “do not modify Claims” leaves claims stuck in `PAYMENT_PENDING`.

| Option | Approach | Recommendation |
|--------|----------|----------------|
| **A (preferred)** | Add thin `ClaimsSettlementListener` in Claims package: on `SettlementCompleted` with `source_module=CLAIMS`, transition `PAYMENT_PENDING → SETTLED → CLOSED` | **Approve as exception** — mirrors `ClaimsWorkflowListener` consuming Workflow events; **zero payment logic** in Claims |
| **B (strict)** | No Claims code changes; document `PAYMENT_PENDING` until a later bridge | Rejects ClaimsDomain §6 completion |

**Plan default if approved:** Option A only (≤1 listener + event type constant). No other Claims changes. No Claims→PSP calls.

---

## 4. Architecture

```text
ClaimApprovedForPayment (EventBus)
        ↓
SettlementIntakeListener (Settlement module)
        ↓
Create Settlement + financial_snapshot_json (immutable)
        ↓
WorkflowRuntime.start(SETTLEMENT_STANDARD, subjectType=SETTLEMENT)
        ↓
Task Engine (FINANCE_REVIEW → APPROVAL → DISBURSE…)
        ↓
Decision Engine (approve/reject/return)
        ↓
SettlementWorkflowListener → domain status
        ↓
PaymentProvider.disburse()  [ManualSettlementProvider in v1]
        ↓
LedgerService.post(DEBIT/CREDIT)  [immutable]
        ↓
Publish SettlementCompleted / SettlementFailed
        ↓
ClaimsSettlementListener (Option A) → Claim SETTLED/CLOSED
        ↓
Communication Engine (templates)
```

**Packages (mirror claims/insurance):**

- `application/settlement/*` — `SettlementService`, `SettlementIntakeService`, `SettlementLifecycleService`, `SettlementNumberGenerator`, `SettlementReportingService`, `SettlementWorkflowListener`, `SettlementIntakeListener`, `LedgerService`, `PaymentProviderRegistry`
- `application/settlement/spi/*` — `PaymentProvider`, `ManualSettlementProvider`, stubs: `BankTransferProvider`, `MobileMoneyProvider`, `GovernmentTreasuryProvider`
- `infrastructure/persistence/settlement/*` — entities/repos for new tables
- `presentation/*Settlement*Controller` + DTOs
- Frontend: settlements dashboard, detail/timeline, finance review, reports

---

## 5. Aggregate Roots

| Aggregate | Owns |
|-----------|------|
| **Settlement** | Number, status, amount, currency, source, beneficiary snapshot, provider code, workflow binding, correlation |
| **LedgerEntry** | Immutable debit/credit/adjustment/reversal/refund lines; `settlement_id`; `entry_no` |
| **SettlementStatusHistory** | Append-only domain transitions |
| **PaymentAttempt** (optional v1 light) | Provider attempt metadata for reconciliation hooks |

---

## 6. Settlement Lifecycle (domain)

```text
DRAFT (rare; intake usually creates PENDING)
  → PENDING                  workflow start / finance queue
  → UNDER_REVIEW             FINANCE_REVIEW task
  → APPROVED                 finance decision APPROVE
  → SENT                     provider.accept / disburse submitted
  → CONFIRMED                provider confirmation (manual mark in v1)
  → COMPLETED                ledger posted + SettlementCompleted
  → FAILED | REJECTED | CANCELLED → CLOSED (soft terminals)
```

Illegal examples: `DRAFT → COMPLETED`; `COMPLETED → SENT`; mutating snapshot after `APPROVED`.

---

## 7. Source Modules (extensibility)

| `source_module` | Intake trigger (v1 / later) |
|-----------------|----------------------------|
| `CLAIMS` | `ClaimApprovedForPayment` (**v1**) |
| `SUBSIDY` | future event |
| `PREMIUM_REFUND` | future |
| `PARTNER_REIMBURSEMENT` | future |
| `FARMER_INCENTIVE` | future |
| `DISASTER_RELIEF` | future |
| `MANUAL` | Finance creates settlement via API (v1 allowed for ops/test) |

Idempotency: unique active settlement per `(source_module, source_record_id)` unless `settlements:admin` override.

---

## 8. Financial Snapshot (frozen at create)

JSON fields (immutable after `APPROVED`):

- `amount`, `currency`, `exchangeRate`
- `beneficiaryName`, `beneficiaryType`, `accountNumber` / `msisdn` / `treasuryRef` (as available)
- `sourceModule`, `sourceRecordId`, `sourceReference` (e.g. claim number)
- `policyId` / `farmerId` / `farmId` / `districtCode` / `cropId` / `insurerId` (denormalized for reports; null-ok)
- `capturedAt`

Claims intake maps from claim + policy beneficiary where present; missing account → Manual provider path still allowed with reason.

---

## 9. Payment Provider SPI

```text
PaymentProvider
  code(): String
  supports(method): boolean
  disburse(SettlementDisburseCommand): PaymentProviderResult
  confirm(externalRef): PaymentProviderResult   // optional
```

| Provider | v1 |
|----------|-----|
| `ManualSettlementProvider` | **Implement** — finance marks sent/confirmed with reference |
| `BankTransferProvider` | Stub |
| `MobileMoneyProvider` | Stub |
| `GovernmentTreasuryProvider` | Stub |

Registry selects by `payment_method` / configured default (`MANUAL`).

---

## 10. Ledger

Table `ledger_entries` (append-only; no UPDATE of amounts):

| Column | Notes |
|--------|-------|
| `entry_type` | `DEBIT`, `CREDIT`, `ADJUSTMENT`, `REVERSAL`, `REFUND` |
| `account_code` | Chart-lite string (e.g. `PAYABLE.CLAIMS`, `CASH.MANUAL`) |
| `amount`, `currency` | Absolute; sign via type |
| `settlement_id` | FK |
| `correlation_id` | Trace |
| `posted_at` | Immutable |

On `COMPLETED`: post balanced pair (e.g. Debit PAYABLE / Credit CASH) in one TX with status transition.  
Reconciliation (later): `reconciliation_refs` / `external_statement_line_id` nullable columns reserved — **no job in v1**.

---

## 11. Workflow binding

Seed published **`SETTLEMENT_STANDARD`** (sequential):

```text
INTAKE → FINANCE_REVIEW → APPROVAL → DISBURSE → CONFIRM → DONE_COMPLETED | DONE_REJECTED | DONE_FAILED
```

- Assignees: `SYSTEM_ADMIN` for bootstrap (same Claims pattern); document remap to `FINANCE_OFFICER` when role is wired.
- Task types: `VERIFICATION` | `APPROVAL` | `PAYMENT`.
- Decisions: reuse catalog `APPROVE` / `REJECT` / `RETURN` / `REQUEST_INFORMATION` (data-only rules if needed).
- Optional thin `SETTLEMENT_FAST_TRACK` for low amount + MANUAL only (debt / optional in same Liquibase).

Settlement selects definition at intake; pins published version via engine.

---

## 12. Events

**Consume:** `ClaimApprovedForPayment`  
**Publish:** `SettlementCreated`, `SettlementStatusChanged`, `SettlementApproved`, `SettlementSent`, `SettlementCompleted`, `SettlementFailed`, `SettlementRejected`  
**Claims (Option A):** consume `SettlementCompleted` / `SettlementFailed`

Register Settlement event types in Communication handler HANDLED set + Liquibase templates (015-style inserts). **Do not** change EventBus interface.

---

## 13. Database (Liquibase `017-settlement-phase7.yaml`)

**New tables:**

- `settlements` — aggregate + snapshot JSON + workflow ids + provider fields + amounts + source keys + indexes `(status)`, `(source_module, source_record_id)`, `workflow_instance_id`, `settlement_number`
- `settlement_status_history`
- `ledger_entries`
- `payment_attempts` (light; provider ref, status, raw_response_json)
- Optional `settlement_types` catalog if needed; else `source_module` + `payment_method` enums as strings

**Permissions:** `settlements:read`, `settlements:write`, `settlements:approve`, `settlements:disburse`, `settlements:admin`  
Map: `SYSTEM_ADMIN` all; seed/map `FINANCE_OFFICER` if role exists or create role; `AUDITOR` read.

**Seeds:** claim types already exist; add workflow defs + notification templates + role maps.

---

## 14. API Plan

| Method | Path | Permission |
|--------|------|------------|
| GET | `/settlements` | `settlements:read` |
| GET | `/settlements/{id}` | `settlements:read` |
| POST | `/settlements` | `settlements:write` | Manual create (`source_module=MANUAL` or admin) |
| GET | `/settlements/{id}/timeline` | `settlements:read` |
| GET | `/settlements/{id}/ledger` | `settlements:read` |
| POST | `/settlements/{id}/manual-confirm` | `settlements:disburse` | Manual provider confirm |
| GET | `/settlements/reports/{name}` | `settlements:read` |

Search: `q`, `status`, `sourceModule`, `provider`, dates, amount band.  
Adjudication via existing `/tasks/**` + decisions (`subjectType=SETTLEMENT`).

---

## 15. UI Plan

| Screen | Route | Permission |
|--------|-------|------------|
| Dashboard / search | `/settlements` | read |
| Detail + timeline + ledger | `/settlements/:id` | read |
| Finance review (deep-link tasks) | claim details pattern | approve via tasks |
| Reports | `/settlements/reports` | read |

Reuse `WorkflowTimeline`, Task Inbox, `DecisionDialog`. Responsive EnterpriseTable layout.

---

## 16. Reporting (v1 aggregates)

- Total paid today / period  
- Pending settlements count + amount  
- Failed payments  
- Average cycle time (created → completed)  
- By provider; by source module  
- By district / crop / insurer when snapshot fields present  

---

## 17. Tests

- `SettlementPlatformIntegrationTest`: claim approve path → `ClaimApprovedForPayment` → settlement created → finance decisions → manual confirm → ledger rows → `SettlementCompleted` → claim `SETTLED`/`CLOSED` (if Option A)  
- Unit: number gen, lifecycle illegal transitions, ledger immutability (no update), idempotent intake  
- Provider stub unit: Manual success path  

---

## 18. Version / docs on implementation

- Bump to **0.12.0**  
- Update CHANGELOG, API.md, Database.md, roadmap (Phase 7 = Settlement; Climate → Phase 8+)  
- This doc → Approved/Implemented  
- Cross-link `ArchitectureGovernance.md`  

---

## 19. Delivery order (implementation todos — after approval only)

1. `docs-governance` — ship `ArchitectureGovernance.md` (can land with design gate)  
2. `db-017` — Liquibase 017 + seeds  
3. `domain-services` — aggregate, ledger, SPI, lifecycle, listeners, number gen  
4. `apis-tests` — controllers, OpenAPI, ITs  
5. `frontend-settlements` — dashboard, detail, reports, RBAC  
6. `docs-version` — 0.12.0 + final 11-point readiness summary  

---

## 20. Technical debt (record at ship)

- No 6E dual-control / SLA  
- Manual provider only  
- Reconciliation jobs deferred  
- FX engine deferred  
- Bootstrap assignees `SYSTEM_ADMIN`  
- Claims closure Option A or B  

---

## 21. Quality gate (this design stage)

- [x] Vision and multi-source extensibility clear  
- [x] Engine consumption rules (no forks)  
- [x] Ledger + SPI + snapshot specified  
- [x] Workflow graph sketched  
- [x] Claims closure decision called out  
- [ ] **Human approval** before Liquibase/code  

### Explicitly deferred to implementation PR(s)

- Liquibase, entities, services, APIs, UI, tests, version bump  
