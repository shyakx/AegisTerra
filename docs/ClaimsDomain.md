# AegisTerra Claims Domain

**Version:** 1.1.0 (Claims Management implemented — Phase 6F / 0.11.0)  
**Status:** Approved / Implemented (v1 sequential `CLAIM_STANDARD` + `CLAIM_FAST_TRACK`)  
**Depends on:** Phase 2 IAM · Phase 4 Agriculture · Phase 5 Insurance Core · Workflow Engine Stages **6A–6D** (delivered) · ideally Stage **6E** (SLA / multi-approver policies) before production dual-control adjudication  
**Related:** `WorkflowArchitecture.md`, `EventArchitecture.md`, `InsuranceDomain.md`, `FarmerDomain.md`, ADR-008  

> **Implementation note:** Phase 6F delivers Claims as the first Workflow Engine consumer without modifying Workflow/Task/Decision/Communication engine source. Parallel/majority/SLA remain engine 6E debt.

---

## 1. Domain Vision

Claims Management is the operational heart of agricultural insurance payouts. Every insured loss event becomes a **Claim** — a governed financial request against an **active (or eligible) InsurancePolicy**, adjudicated through the **shared Workflow Engine**, evidenced by documents and assessments, and culminating in an **approved indemnity amount** (or rejection) that Settlement (later phase) can pay.

Claims must:

1. Scale to **national portfolios** (high intake volume, multi-insurer, multi-district field force).
2. Remain **fair, auditable, and dual-control ready** for government and insurer oversight.
3. Support **parametric (index)** and **indemnity (loss assessment)** products without forking the platform.
4. Treat the Workflow Engine as the **only** orchestration runtime — Claims owns domain state and money rules, **never** a private task/approval graph.
5. Emit domain events so Communication, Analytics, and Settlement stay loosely coupled.

**Non-goals (this design / first Claims release):**

- Payment gateway / bank settlement ledger (Settlement phase).
- Full ML model training ops (AI Risk Review is a **touchpoint** with SPI; models live outside Claims).
- Replacing PolicyStatus or Farmer registration workflows.
- Hardcoding claim step names inside Workflow Engine Java.

---

## 2. Business Goals

| Goal | Success signal |
|------|----------------|
| Faster cycle time | Median Submitted → Decision within configured SLA (by claim type / district) |
| Accurate payouts | Assessed amount ≤ coverage residual; waiting periods & exclusions enforced |
| Fraud containment | High-risk claims forced through enhanced inspection / dual approval |
| Farmer trust | Transparent status, notifications, evidence checklist, appeal path (v2) |
| Regulatory readiness | Immutable timeline + audit for every material transition |
| Operational clarity | Role-based inbox (adjuster, inspector, supervisor, finance) via Workflow tasks |
| Extensibility | New claim types added via catalog + workflow definition versions — not code forks |

---

## 3. Bounded Context

| Context | Owns | Does not own |
|---------|------|--------------|
| **Claims** | Claim aggregate, claim types, cause of loss, claimed/assessed/approved amounts, assessments, evidence metadata links, fraud flags, domain status machine, claim-number series, claim documents catalog rules | Task inbox UX rules, approval majority math, email transport, policy premium math, bank transfers |
| **Workflow** | Definitions (`CLAIM_*`), instances, tasks, decisions, timeline, escalations (6E+) | Claim amount caps, fraud scores |
| **Insurance** | Policy, coverage limits, exclusions, waiting periods, beneficiaries | Claim intake forms |
| **Agriculture** | Farmer, Farm, boundary, crop seasons | Claim status |
| **Climate / Risk** (read) | Weather stations, indices, risk scores used as verification inputs | Claim decisions |
| **Engagement** | Document/attachment storage blobs | Whether a document type is *required* for a claim type |
| **Identity** | Users, roles (`claims:read/write/assess/decide/admin`), assignment targets | Claim business rules |
| **Settlement** (future) | Payouts, ledgers, PSP | Creating claims |

**Integration rules:**

- Claims references Policy / Farmer / Farm / Season / Crop by **UUID only**.
- Claims stores `workflow_instance_id` and listens to Workflow / Decision / Task events via `EventBus`.
- Claims **never** updates `workflow_*` tables directly.
- Workflow **never** writes claim amounts; it only signals step outcomes Claims maps to domain status.

---

## 4. Aggregate Roots

| Aggregate | Consistency boundary | Contained / dependent |
|-----------|----------------------|------------------------|
| **Claim** | Financial request + domain status | CauseOfLoss, ClaimedItems, StatusHistory (append), FraudSignal refs, links to assessments/evidence |
| **ClaimType** | Catalog of admissible claim kinds | Default workflow definition code, required document matrix, assessment profile |
| **ClaimAssessment** | One assessment cycle (may repeat) | Methods used, scores, recommended amount, assessor, notes |
| **ClaimEvidence** | Logical evidence package entry | Document/attachment ids, GPS, media type, capture metadata |
| **ClaimInspection** | Field or remote inspection record | Schedule, inspector, findings, geo check-in |
| **ClaimDecisionRecord** | Domain mirror of adjudication outcome | Approved/rejected amount, reason codes — *complements* Workflow Decision Engine history |
| **ClaimDraft** | Wizard progress before submit | Serialized step payloads (like PolicyIssuanceDraft) |

**Application rule:** One aggregate root mutated per transaction unless a documented saga (e.g. **SubmitClaim**) coordinates Claim + `workflowRuntime.start` + audit under one `correlationId`.

### Phase 3 tables already present (scaffold)

`claims`, `claims_history`, and related insurance tables exist from Phase 3 schema. Implementation (after approval) will **extend** them — not invent a parallel claims store — with columns for type, workflow binding, amounts, fraud, etc.

---

## 5. Entity Relationships

```text
InsurancePolicy 1──* Claim
Farmer            1──* Claim          (denormalized from policy for query)
Farm              0..1──* Claim       (loss location; may differ from policy farm in edge cases — rule-gated)
Season / Crop     0..1──* Claim
ClaimType         1──* Claim
Claim             1──* ClaimAssessment
Claim             1──* ClaimEvidence
Claim             0..* ClaimInspection
Claim             0..* ClaimDecisionRecord (domain)
Claim             0..1 WorkflowInstance   (subject_type=CLAIM, subject_id=claim.id)
ClaimEvidence     *──1 Document/Attachment (Engagement)
ClaimAssessment   *──0..1 ClaimInspection
```

Optional future: `ClaimAppeal` (v2), `ClaimReserve` (actuarial), `ClaimRecovery` (salvage/subrogation).

---

## 6. Claim Lifecycle

Domain status is the **Claims source of truth** for business reporting. Workflow instance state is the **orchestration source of truth** for who must act next.

```text
DRAFT
  │ submit (validates policy + docs minimum)
  ▼
SUBMITTED ──────────────────────► workflowRuntime.start(CLAIM_*)
  │
  ▼
UNDER_VALIDATION          ◄──── workflow step VALIDATION / tasks
  │ pass / fail-with-request
  ▼
ASSIGNED                  ◄──── adjuster / pool assignment
  │
  ▼
INSPECTION                ◄──── optional by type / fraud score
  │
  ▼
ASSESSMENT
  │
  ▼
PENDING_DECISION          ◄──── Decision Engine on approval task(s)
  │
  ├──► APPROVED ──► PAYMENT_PENDING ──► SETTLED ──► CLOSED
  │
  ├──► REJECTED ───────────────────────────────► CLOSED
  │
  ├──► RETURNED_FOR_INFO ──► (back to SUBMITTED / VALIDATION)
  │
  └──► CANCELLED / WITHDRAWN ──────────────────► CLOSED
```

**Mirroring rule:** Claims application listeners map workflow terminal / milestone events → domain status. UI shows **domain status** primarily; Timeline embeds **workflow + decision + notification** history.

Settlement phase owns moving `PAYMENT_PENDING` → `SETTLED` when funds clear; until then Claims may set `PAYMENT_PENDING` when approval completes and emit `ClaimApprovedForPayment`.

---

## 7. State Machine

### 7.1 Domain statuses

| Status | Meaning | Terminal? |
|--------|---------|-----------|
| `DRAFT` | Wizard / incomplete intake | No |
| `SUBMITTED` | Accepted intake; workflow starting/started | No |
| `UNDER_VALIDATION` | Completeness / eligibility checks | No |
| `ASSIGNED` | Owner adjuster or ROLE pool active | No |
| `INSPECTION` | Field/remote inspection in progress | No |
| `ASSESSMENT` | Loss quantification in progress | No |
| `PENDING_DECISION` | Awaiting formal approve/reject (Decision Engine) | No |
| `RETURNED_FOR_INFO` | Gaps; claimant/operator must supply data | No |
| `APPROVED` | Indemnity authorized (amount frozen) | No* |
| `REJECTED` | Denied with reason codes | Soft terminal → `CLOSED` |
| `PAYMENT_PENDING` | Handed to Settlement | No |
| `SETTLED` | Paid / closed financially | Soft terminal → `CLOSED` |
| `CANCELLED` | Withdrawn / voided before payout | Soft terminal → `CLOSED` |
| `CLOSED` | No further domain transitions except audit notes | Yes |

\* `APPROVED` may skip straight to `CLOSED` for zero-pay edge cases (policy design); normally → `PAYMENT_PENDING`.

### 7.2 Illegal transitions (examples)

- `DRAFT` → `APPROVED` (must pass workflow).
- `REJECTED` → `APPROVED` without new workflow / reopen policy (v2 appeal).
- `SETTLED` → any non-`CLOSED` reopen without `claims:admin` + reason.
- Any status change **only** via `ClaimLifecycleService` (no repository status writes from controllers).

### 7.3 Workflow binding

| ClaimType profile | Workflow definition code (seed target) |
|-------------------|----------------------------------------|
| Standard indemnity | `CLAIM_STANDARD` |
| Parametric / weather index | `CLAIM_INDEX` |
| Government disaster | `CLAIM_DISASTER` |
| Fast-track low amount | `CLAIM_FAST_TRACK` |

Definition graphs encode VALIDATION → INSPECTION? → ASSESSMENT → DECISION. Claims selects definition by type + amount band + fraud tier at **submit** time (pinned published version).

---

## 8. Business Rules

1. **Policy eligibility:** Claimable only if policy status ∈ {`ACTIVE`, `EXPIRED` within grace} per product rules; not `CANCELLED` / `DRAFT`.
2. **Coverage window:** `incident_date` within policy effective dates + waiting period satisfied for peril.
3. **Peril match:** Claim type / cause must map to covered peril; exclusions deny or reduce.
4. **Sum insured residual:** `approved_amount ≤ remaining coverage` (policy limit − prior settled claims for same period/peril).
5. **One open claim per peril-period (configurable):** Prevent duplicate open claims for same policy + peril + season unless `claims:admin` override.
6. **Currency:** Claim currency = policy currency.
7. **Beneficiary:** Payout party defaults to policy beneficiary / farmer; override audited.
8. **Assessment ceiling:** Recommended amount cannot exceed claimed amount unless product allows “reassessment uplift” (default: no).
9. **Dual control:** Amounts over threshold `T` require Decision Engine outcome with supervisor role (config).
10. **Fraud hold:** If fraud tier ≥ `HIGH`, inspection mandatory; auto fast-track disabled.
11. **Index claims:** Payout formula from product index parameters + official weather series; manual override requires reason + `claims:decide`.
12. **Soft delete:** Claims soft-deleted; history retained; open workflow cancelled via runtime API.

---

## 9. Validation Rules

### 9.1 Create / draft

| Field | Rule |
|-------|------|
| `policyId` | Required, exists, caller authorized for farmer/org |
| `claimTypeCode` | Required, active catalog |
| `incidentDate` | Required, not future, ≥ policy start − configured lookback |
| `description` | Required on submit (≥ N chars) |
| `claimedAmount` | ≥ 0; required for indemnity types; optional/zero for pure index until calculated |
| `lossLocation` | Farm id or GPS required when type demands geo |
| `seasonId` / `cropId` | Required when product is crop-linked |

### 9.2 Submit

- Minimum evidence set for claim type satisfied (see §10).
- Identity verification flag on farmer not `REJECTED`.
- No blocking open fraud case (unless override).
- Workflow definition resolvable and published.

### 9.3 Assessment

- At least one assessment method recorded.
- `recommendedAmount` numeric, currency match, ≤ residual cover.
- Photos geolocation vs farm boundary: warn if distance > threshold (configurable); block only if rule severity = `HARD`.

### 9.4 Decision

- Decision Engine validates type/outcome/comment rules.
- Domain rejects approve if amount missing or guards fail (`ClaimWithinCoverageGuard`, `ClaimFraudClearedGuard`).

---

## 10. Required Documents

Document **requirements** are Claims catalog; **bytes** live in Engagement.

| Document type | Typical claim types | Required? |
|---------------|---------------------|-----------|
| Claim form (structured) | All | Yes on submit |
| Photos (loss / crop / livestock) | Yield, flood, drought, pest, livestock, manual | Yes (≥ N) |
| Videos | Flood, disaster, livestock | Optional / type-gated |
| GPS coordinates / geotagged media | Field losses | Yes when geo rule on |
| Inspection report | When inspection step ran | Yes before decision if inspection required |
| Farmer statement | Manual, dispute-prone | Optional / type-gated |
| Third-party docs (vet, agronomist, local authority) | Livestock, disaster, pest | Type-gated |
| Weather bulletin / index certificate | Weather index, drought, flood | System-attached preferred |
| Satellite scene refs | Yield / drought / flood | System or analyst attach |
| National ID / KYC snapshot | All (reference) | Soft — from Farmer profile |

Evidence metadata must capture: `capturedAt`, `capturedBy`, `source` (`MOBILE`|`WEB`|`SYSTEM`|`IMPORT`), `checksum`, optional `geometry`.

---

## 11. Assessment Workflow

Assessment is a **domain process** executed while the Workflow instance sits on an `ASSESSMENT` (or `INSPECTION`) step. Completing the domain assessment enables the human/system to complete the workflow task (optionally with `advanceAction`).

### 11.1 Methods (composable)

| Method | Description | Producer |
|--------|-------------|----------|
| `FIELD_INSPECTION` | On-site visit, checklist, GPS check-in | Inspector task |
| `SATELLITE_VERIFICATION` | Scene compare / NDVI / flood extent | Climate/GIS service SPI |
| `WEATHER_VERIFICATION` | Station/index thresholds | Climate read models |
| `PHOTO_EVIDENCE` | Gallery review + EXIF/GPS checks | Adjuster |
| `DOCUMENT_REVIEW` | Statements, vet notes, authority letters | Adjuster |
| `AI_RISK_REVIEW` | Model score + explanation refs | AI SPI (async) |
| `MANUAL_OVERRIDE` | Human sets amount/reason overriding suggestions | `claims:decide` |

### 11.2 Assessment profile per claim type

| Claim type | Default methods | Inspection default |
|------------|-----------------|--------------------|
| Weather Index | Weather + Document | Rare |
| Yield Loss | Photo + Satellite + Field (if high) | Conditional |
| Flood | Photo + Satellite + Weather + Field | Often |
| Drought | Weather + Satellite + Photo | Conditional |
| Pest & Disease | Photo + Field + Third-party | Often |
| Livestock | Photo/Video + Vet doc + Field | Often |
| Manual | Document + Photo + Manual override | As assigned |
| Government Disaster | Authority docs + Weather/Satellite + Dual decision | As mandated |

### 11.3 Output

`ClaimAssessment` stores: methods[], raw findings JSON, `recommendedAmount`, `confidence`, `fraudHints[]`, `assessorId`, timestamps. Multiple assessments allowed; **latest accepted** feeds decision.

---

## 12. Approval Workflow

Approvals are **not** a Claims-owned queue.

```text
Assessment complete
      ↓
Workflow task type APPROVAL / DECISION step
      ↓
Decision Engine (types/outcomes/rules)
      ↓
decision_rules.workflow_action → graph transition
      ↓
Claims listener maps outcome → APPROVED | REJECTED | RETURNED_FOR_INFO
```

### 12.1 Decision catalog (Claims-seeded configuration — not engine hardcode)

| Decision type | Outcomes (examples) | Notes |
|---------------|---------------------|-------|
| `CLAIM_ADJUDICATION` | `APPROVE_FULL`, `APPROVE_PARTIAL`, `REJECT`, `RETURN_INFO` | Partial requires amount |
| `CLAIM_INSPECTION_ACCEPT` | `ACCEPT`, `REWORK` | Inspection QA |
| `CLAIM_FRAUD_REVIEW` | `CLEAR`, `CONFIRM_FRAUD`, `ESCALATE` | Blocks payout if confirm |

### 12.2 Amount thresholds (config)

| Band | Policy |
|------|--------|
| ≤ T1 | Single adjuster decision |
| T1–T2 | Sequential supervisor |
| > T2 | Parallel/majority (needs Workflow Stage **6E** approval policies) |

Until 6E ships, implement sequential single/supervisor only; document parallel as **future binding**.

### 12.3 Guards (Claims SPI registered with Workflow)

- `ClaimWithinCoverageGuard`
- `ClaimDocumentsCompleteGuard`
- `ClaimFraudClearedGuard`
- `ClaimAssessmentPresentGuard`

---

## 13. Fraud Detection Touchpoints

Fraud is a **cross-cutting signal**, not a separate product UI in v1.

| Touchpoint | When | Action |
|------------|------|--------|
| Intake scoring | On submit | Tier LOW/MED/HIGH; set `fraud_tier`; may force `CLAIM_STANDARD` over fast-track |
| Geo mismatch | Evidence upload | Flag if photo GPS far from farm boundary |
| Duplicate detection | Submit | Same policy + peril + overlapping dates |
| Velocity | Submit | N claims / farmer / season |
| Weather inconsistency | Assessment | Claimed drought while index shows surplus rain |
| AI risk review | Async after submit/assessment | Update score; optional auto-task `VERIFICATION` |
| Manual fraud review | Decision / dedicated step | Outcome blocks `APPROVED` |
| Post-settle audit | Settlement phase | Recovery / blacklist (future) |

Claims stores `fraud_tier`, `fraud_score`, `fraud_flags[]`. Models behind `FraudScoringPort` (stub allowed in first implementation).

---

## 14. Integration Points

| System | Direction | Contract |
|--------|-----------|----------|
| **WorkflowRuntime** | Claims → Engine | `start`, listen complete/cancel; complete tasks with actions |
| **Decision Engine** | Via tasks | Record decisions; no Claims switch on outcomes beyond mapping table |
| **EventBus / Communication** | Claims publishes domain events | Templates for submitted/approved/rejected/returned |
| **Insurance Policy** | Read | Eligibility, limits, exclusions, waiting periods |
| **Agriculture** | Read | Farmer, farm, boundary, crop season |
| **Engagement Documents** | Write/read | Evidence blobs |
| **Climate / Weather** | Read SPI | Index series, alerts |
| **GIS / Satellite** | Read SPI | Scene metadata, derived loss index |
| **AI Risk** | Async SPI | Score + explanation |
| **Settlement** (future) | Claims → Settlement | `ClaimApprovedForPayment` event / API |
| **National ID / KYC** | Read | Soft checks |
| **Mobile field app** (future) | Same APIs | Inspection + evidence capture |

---

## 15. Reporting Requirements

| Report | Grain | Consumers |
|--------|-------|-----------|
| Claims intake volume | Day/week × type × district | Ops, ministry |
| Cycle time funnel | Status durations | Ops |
| Approval rate / rejection reasons | Type × insurer | Compliance |
| Amount claimed vs approved vs settled | Portfolio | Finance, actuarial |
| Fraud tier distribution | Type × region | Risk |
| SLA breach (open tasks overdue) | Definition × step | Ops (needs 6E SLA) |
| Outstanding reserves (approved unpaid) | Insurer | Finance |
| Per-policy claim history | Policy | Underwriting |

APIs: search + aggregate endpoints; export CSV async later. No BI warehouse in v1.

---

## 16. Audit Requirements

Every material action must write:

1. **IAM audit** (`AuditAction` — new claims actions).
2. **Claims status history** (append-only domain history table).
3. **Workflow timeline** (engine events).
4. **Decision history** (Decision Engine).
5. **Notification log** (Communication Engine).

Audited events include: draft save, submit, evidence add/remove, assessment create, inspection complete, decision, amount change, fraud tier change, cancel, reopen (admin), soft delete.

Retention: soft delete + history immutable; purge only via regulated job (out of scope).

Correlation: all writes share `correlationId` from submit or task action.

---

## 17. Claim Types (extensibility)

| Code | Nature | Notes |
|------|--------|-------|
| `WEATHER_INDEX` | Parametric | Threshold triggers; minimal inspection |
| `YIELD_LOSS` | Indemnity | Yield measurement / satellite assist |
| `FLOOD` | Indemnity | Geo + weather + photos |
| `DROUGHT` | Indemnity / hybrid | Index assist common |
| `PEST_DISEASE` | Indemnity | Agronomist docs |
| `LIVESTOCK` | Indemnity | Vet certification |
| `MANUAL` | Catch-all | Operator-defined peril |
| `GOVERNMENT_DISASTER` | Special | Authority declaration ref mandatory |

**Extension model:** insert `claim_types` row + document matrix + assessment profile + workflow definition code. **No** new Java enum required for routing if codes stay stringly-configured; typed enums optional for compile-time safety on first release set.

---

## 18. Workflow Integration (summary)

```text
SubmitClaim
  → persist Claim (SUBMITTED)
  → resolve definitionCode
  → workflowRuntime.start(definitionCode, subjectType=CLAIM, subjectId)
  → store workflow_instance_id

Engine runs tasks/decisions/notifications…

ClaimsWorkflowListener
  → on step/terminal events: map → domain status
  → on DecisionRecorded (optional): sync ClaimDecisionRecord

Claims never:
  → creates workflow_tasks rows
  → implements approval majority
  → sends SMS/email directly
```

Seed definitions (`CLAIM_STANDARD`, etc.) are a **pre-implementation** engine checklist item (WorkflowArchitecture 6F); this design specifies **what** they must express, not their JSON.

---

## 19. API Plan (design only — do not implement)

**Base:** `/api/v1`  
**Auth:** JWT cookies + RBAC  

| Method | Path | Permission | Purpose |
|--------|------|------------|---------|
| GET | `/claims` | `claims:read` | Search/filter/page |
| GET | `/claims/{id}` | `claims:read` | Detail + summary |
| POST | `/claims/drafts` | `claims:write` | Create draft |
| PUT | `/claims/drafts/{id}` | `claims:write` | Update draft |
| POST | `/claims/drafts/{id}/submit` | `claims:write` | Validate + start workflow |
| POST | `/claims` | `claims:write` | Direct create+submit (API clients) |
| POST | `/claims/{id}/cancel` | `claims:write` | Withdraw + cancel workflow |
| GET | `/claims/{id}/timeline` | `claims:read` | Merged domain + workflow timeline |
| GET/POST | `/claims/{id}/evidence` | `claims:write` / read | Evidence package |
| POST | `/claims/{id}/assessments` | `claims:assess` | Record assessment |
| GET | `/claims/{id}/assessments` | `claims:read` | List |
| POST | `/claims/{id}/inspections` | `claims:assess` | Schedule/complete inspection |
| GET | `/claim-types` | `claims:read` | Catalog |
| GET | `/claims/reports/{name}` | `claims:read` | Reporting aggregates |

Task act / decide remain on existing `/api/v1/tasks/**` and decision endpoints — Claims UI composes them.

**Events published (Claims):** `ClaimDraftSaved`, `ClaimSubmitted`, `ClaimStatusChanged`, `ClaimAssessmentRecorded`, `ClaimApprovedForPayment`, `ClaimRejected`, `ClaimCancelled`.

---

## 20. UI Plan (design only — do not implement)

| Screen | Purpose | Primary roles |
|--------|---------|---------------|
| **Claim Dashboard** | Queues by status, SLA badges, filters (type, district, insurer) | Adjuster, supervisor, ops |
| **Claim Wizard** | Multi-step intake (policy → loss → evidence → review → submit) | Officer, aggregator, farmer portal later |
| **Claim Details** | Header status, amounts, policy snapshot, actions | All with read |
| **Inspection View** | Checklist, map, check-in, findings | Inspector |
| **Evidence Gallery** | Photos/videos/docs with GPS map pins | Adjuster, inspector |
| **Assessment View** | Methods, recommended amount, AI panel, override | Adjuster |
| **Decision History** | Domain + Decision Engine outcomes | Supervisor, auditor |
| **Timeline** | Shared `<WorkflowTimeline />` + claim status history | All |

Reuse: Notification Center, Task Inbox, Decision dialog from Stages 6B–6D. Replace current Claims scaffold `useState` mock.

### Navigation

- Sidebar: Claims (permission `claims:read`).
- Deep links from Policy Details → “File claim” / “Claims on policy”.

---

## 21. Proposed schema extensions (post-approval only)

*Listed for planning — **no Liquibase in this stage**.*

- `claims`: `claim_type_code`, `farmer_id`, `farm_id`, `season_id`, `crop_id`, `cause_of_loss`, `assessed_amount`, `approved_amount`, `workflow_instance_id`, `fraud_tier`, `fraud_score`, `submitted_at`, `closed_at`, `reason_code`, …
- `claim_types`, `claim_type_document_rules`, `claim_assessments`, `claim_evidence`, `claim_inspections`, `claim_status_history`, `claim_drafts`
- Permissions: `claims:read`, `claims:write`, `claims:assess`, `claims:decide`, `claims:admin` (seed role maps for INSURANCE_OFFICER, etc.)

---

## 22. Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Implementing Claims before Workflow **6E** (parallel/majority + SLA) | Weak dual-control & overdue handling for high-value claims | Gate high-amount types; sequential only until 6E |
| Duplicating task tables in Claims | Platform fragmentation | Hard review gate; reuse inbox |
| Status drift (domain vs workflow) | Operator confusion | Single mapper service + reconciliation job |
| Index vs indemnity product mix | Wrong assessment path | ClaimType → definition + profile matrix |
| Fraud false positives | Farmer harm | Human override + audit; tiered not binary |
| Satellite/AI vendor lock-in | Cost / outage | SPI ports + manual methods always available |
| Evidence storage cost | Budget | Quotas, compression, retention policy |
| Concurrent assessments | Amount races | Optimistic locking on Claim; assessment “accepted” flag |
| Existing scaffold UI mocks | False production confidence | Delete mocks in implementation PR |
| Policy residual calculation bugs | Overpayment | Shared CoverageResidual service + tests |
| Notification noise | Alert fatigue | Templates + preferences (6D) |

---

## 23. Quality Gate (this design stage)

### Must be true before implementation approval

- [x] Domain vision and bounded context clear vs Workflow / Insurance  
- [x] Aggregates and relationships defined  
- [x] Domain lifecycle + state machine specified  
- [x] Business / validation / document rules listed  
- [x] Assessment & approval designed on **engine**, not a private BPM  
- [x] Fraud touchpoints identified  
- [x] Integration, reporting, audit requirements listed  
- [x] API plan and UI plan documented  
- [x] Claim types extensible without engine forks  
- [x] **No code, migrations, controllers, or FE** delivered in this stage  

### Explicitly deferred to implementation PR(s)

- Liquibase, entities, services, APIs, UI  
- Seeding `CLAIM_*` workflow graphs (coordinate with engine 6F checklist)  
- Real AI / satellite providers  

---

## 24. Architecture Summary (for reviewers)

1. **Claims owns money and eligibility; Workflow owns orchestration.**  
2. **Submit** starts a pinned workflow definition chosen by claim type / risk band.  
3. **Assessment & inspection** are domain records completed under workflow tasks.  
4. **Approve/reject** goes through the **Decision Engine**; Claims only maps outcomes → status/amounts.  
5. **Documents** requirements are Claims catalog; storage is Engagement.  
6. **Fraud** is scored at touchpoints and can force heavier paths.  
7. **Settlement** consumes `PAYMENT_PENDING` later — not in this domain’s first cut.  
8. **UI** is dashboard + wizard + detail + inspection/evidence/assessment + shared timeline/inbox.  
9. **API** is CRUD/search for claims plus evidence/assessment; tasks/decisions stay on engine APIs.  
10. **Do not implement** until this document is approved and engine readiness (incl. preferred 6E SLA/policies) is accepted for the claim types going live.

---

## 25. Review Checklist — Approval Block

**Awaiting stakeholder approval.**

Upon approval, implementation should proceed as a dedicated Claims stage (WorkflowArchitecture **6G** / roadmap Claims consumer), with PR checklist:

1. Schema extensions + permissions  
2. Domain services + lifecycle  
3. Workflow listeners + guards + definition seeds  
4. APIs + OpenAPI  
5. UI replacing mocks  
6. Integration tests (claim submit → task → decision → APPROVED)  
7. No Claims-specific logic added inside Workflow Engine modules  

**Stop here — wait for approval before any Claims implementation.**
