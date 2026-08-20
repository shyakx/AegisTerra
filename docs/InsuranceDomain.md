# AegisTerra Insurance Domain (Insurance Core)

**Version:** 5.0.0 (Phase 5 design)  
**Status:** Awaiting approval for implementation  
**Depends on:** Phase 2 IAM, Phase 3 data platform, Phase 4 Agricultural Core (`FarmerDomain.md`)  
**Out of scope:** Claims intake, claim assessment, payouts, settlement accounting (Phase 6+ / Claims Management)

This document is the **design gate** for Phase 5. Services, APIs, UI, and premium formulas must conform to it.  
**No implementation before this document is accepted.**

---

## 1. Purpose

AegisTerra’s insurance core treats every policy as a **financial contract**, not a CRUD row.

It must:

1. Catalog **insurable products** (policy types, coverage packages, limits, exclusions, waiting periods).
2. Issue and manage policies against **verified farmers, farms, seasons, and crops** (Phase 4 identity).
3. Enforce a **strict policy lifecycle state machine** (draft → active → terminal).
4. Price premiums via a **configurable strategy engine** (no hardcoded formula in code paths).
5. Produce **versioned policy documents** (certificate, terms, coverage summary) with QR + digital-signature readiness.
6. Remain the upstream contract for **Claims, Payouts, Analytics, and Regulatory reporting**.

Design for **national portfolio scale**: indexed search, immutable premium quotes once accepted, audit of every material mutation, soft delete, and no mock data on production paths.

---

## 2. Bounded Context

| Context | Owns | Does not own |
|---------|------|--------------|
| **Insurance** | Products, policy types, coverage packages, policies, beneficiaries, premiums, premium quotes, policy documents, underwriting drafts, portfolio reports | Farmer PII, farm geometry, weather raw ingest, claim adjudication, bank settlement |
| **Agriculture** (upstream) | Farmer, Farm, Crop, Season, CropSeason | Policy status |
| **Party** (upstream) | InsuranceCompany, Partner, Aggregator | Premium calculation rules |
| **Climate & Risk** (read) | RiskScore, weather aggregates used as pricing inputs | Policy issuance |
| **Engagement** | Document/Attachment storage primitives | Underwriting decisions |
| **Identity** | Users/roles; `policies:read` / `policies:write` (+ future `policies:approve`) | Product catalog content |

**Integration rule:** Insurance references Agriculture and Party by **UUID only**. Never mutate agri aggregates from insurance services except documented side-effects (e.g. none in Phase 5).

---

## 3. Aggregate Roots

| Aggregate | Consistency boundary | Contained / dependent |
|-----------|----------------------|------------------------|
| **InsuranceProduct** | Marketable product definition | Links to PolicyType(s); metadata, eligibility rules JSON |
| **PolicyType** | Coverage template | Default coverage rules, waiting periods, exclusion refs |
| **CoveragePackage** | Bundled limits/coverages for a product | CoverageLimit lines, Exclusion refs |
| **InsurancePolicy** | The financial contract | Beneficiaries, Premiums, Coverage snapshot, Exclusions snapshot, WaitingPeriods snapshot, Documents |
| **Premium** | Amount due/paid for a policy period | Calculation snapshot id |
| **PremiumQuote** | Immutable priced offer before activation | Factor breakdown JSON |
| **PolicyIssuanceDraft** | Wizard progress | Serialized step payloads |
| **PolicyDocument** | Generated artifact version | Attachment/content hash, QR payload |

**Application rule:** One aggregate root mutated per transaction unless a documented multi-aggregate use case (e.g. **issue/activate policy**) coordinates Policy + Premium + Document with clear ordering and a single audit correlation id.

### Phase 3 tables reused

- `policy_types`, `insurance_policies`, `premiums`, `insurance_policies_history`
- `insurance_companies` (Party)
- `documents` / `attachments` (Engagement)

### Phase 5 schema additions (Liquibase after approval)

| Table / column | Purpose |
|----------------|---------|
| `insurance_products` | Product catalog |
| `coverage_packages` | Packages under product/type |
| `coverage_limits` | Sum insured / per-peril limits |
| `policy_exclusions` | Catalog + policy snapshot rows |
| `waiting_periods` | Catalog + policy snapshot rows |
| `policy_beneficiaries` | Named beneficiaries per policy |
| `premium_quotes` | Immutable quotes + factor breakdown |
| `premium_pricing_rules` | Configurable rule definitions (strategy inputs) |
| `policy_issuance_drafts` | Wizard drafts |
| `policy_documents` | Versioned generated docs |
| `insurance_policies.crop_season_id` | Link to planting context |
| `insurance_policies.product_id` / `coverage_package_id` | Product binding |
| `insurance_policies.premium_quote_id` | Accepted quote |
| `insurance_policies.parent_policy_id` | Renewal chain |
| `insurance_policies.reason` / transition metadata | Suspension/cancel reasons |
| Extended status vocabulary | Align with state machine below |

Claims/payout tables exist but **must not** gain workflow services in Phase 5.

---

## 4. Entity Relationships

```text
InsuranceCompany 1──* InsurancePolicy
InsuranceProduct 1──* PolicyType
InsuranceProduct 1──* CoveragePackage
PolicyType 1──* InsurancePolicy
CoveragePackage 0..1──* InsurancePolicy
Farmer 1──* InsurancePolicy
Farm 1──* InsurancePolicy
CropSeason 0..1──* InsurancePolicy
Season / Crop ← via CropSeason or denormalized search fields
InsurancePolicy 1──* PolicyBeneficiary
InsurancePolicy 1──* Premium
InsurancePolicy 0..1── PremiumQuote (accepted)
InsurancePolicy 1──* PolicyDocument
InsurancePolicy 0..1── InsurancePolicy (parent / renewed-from)
CoveragePackage 1──* CoverageLimit
PolicyType / Package ──* Exclusion (catalog)
PolicyType / Package ──* WaitingPeriod (catalog)
PolicyIssuanceDraft → created_by user; optional farmer/farm refs in payload
```

---

## 5. State Machine (Policy Lifecycle)

Canonical statuses (stored on `insurance_policies.status`):

```text
DRAFT
  → SUBMITTED
      → UNDER_REVIEW
          → APPROVED
              → PREMIUM_PENDING
                  → ACTIVE
                      → SUSPENDED → ACTIVE
                      → EXPIRED
                          → RENEWED   (new policy id; parent links)
                      → CANCELLED
          → REJECTED          (terminal for this application)
  → CANCELLED                 (withdrawn before issue)
ACTIVE → EXPIRED | CANCELLED | SUSPENDED
SUSPENDED → ACTIVE | CANCELLED | EXPIRED
PREMIUM_PENDING → ACTIVE | CANCELLED | EXPIRED
```

### Transition matrix (enforced in domain/application)

| From | To | Guard |
|------|----|-------|
| DRAFT | SUBMITTED | Farmer/farm/product/package/season/crop present; quote attached or recalculated |
| SUBMITTED | UNDER_REVIEW | Reviewer assignment optional; auto-enter on submit in Phase 5 |
| UNDER_REVIEW | APPROVED | `policies:approve` (or `policies:write` until role split); eligibility checks pass |
| UNDER_REVIEW | REJECTED | Reason required |
| APPROVED | PREMIUM_PENDING | Premium schedule created from accepted quote |
| PREMIUM_PENDING | ACTIVE | Premium marked PAID (or government subsidy fully covers + confirmation); start/end dates valid; documents generated |
| ACTIVE | SUSPENDED | Reason + actor; no new claims later phases while suspended |
| SUSPENDED | ACTIVE | Reason; still within coverage dates |
| ACTIVE / SUSPENDED | CANCELLED | Reason; cancel effective date ≤ end_date |
| ACTIVE / SUSPENDED / PREMIUM_PENDING | EXPIRED | `end_date < today` (job or on-read transition command) |
| EXPIRED | RENEWED | Creates **new** policy in DRAFT/SUBMITTED with `parent_policy_id`; parent stays EXPIRED |
| DRAFT / SUBMITTED / UNDER_REVIEW | CANCELLED | Withdrawal |

Illegal transitions → HTTP **409 Conflict** (or **422** when guard fails validation).

**Renewed** is a **relationship state of the successor**, not a lingering status on the parent. Parent remains `EXPIRED` (or `CANCELLED` if non-renewed termination). UI timeline shows “Renewed as POL-…”.

---

## 6. Domain Events

Emitted after successful commit (persist to `audit_logs` + optional in-process listeners):

| Event | Payload highlights |
|-------|-------------------|
| `ProductCreated` / `Updated` | id, code |
| `PolicyTypeCreated` / `Updated` | id, code |
| `CoveragePackageCreated` / `Updated` | id, productId |
| `PremiumQuoted` | quoteId, farmerId, farmId, gross/net, factors hash |
| `PolicyDraftSaved` | draftId, step |
| `PolicySubmitted` | policyId, policyNumber |
| `PolicyUnderReview` | policyId |
| `PolicyApproved` / `Rejected` | policyId, actor, reason |
| `PremiumPending` | policyId, premiumId |
| `PolicyActivated` | policyId, documentIds |
| `PolicySuspended` / `Reinstated` | policyId, reason |
| `PolicyCancelled` | policyId, reason |
| `PolicyExpired` | policyId |
| `PolicyRenewalStarted` | parentPolicyId, newPolicyId |
| `PolicyDocumentGenerated` | policyId, documentType, version |

Audit store includes: actor, timestamp, action, resource, correlation id, **old/new JSON**, optional **reason**.

---

## 7. Business Rules

### Product / type / package
- Product `code` unique among non-deleted.
- PolicyType `code` unique; may map to multiple products over time but one active binding per policy.
- CoveragePackage belongs to one product; activating a package freezes its limit/exclusion/waiting-period **snapshot** onto the policy at approval.
- Disabled products cannot be selected for new issuance (existing ACTIVE policies remain).

### Policy (contract)
- `policy_number` unique among non-deleted; system-generated (`POL-YYYY-########`) unless regulator override.
- Must reference existing non-deleted Farmer + Farm; farm must belong to farmer.
- Prefer `crop_season_id` linking Crop×Season×Farm; if absent, crop + season required on draft payload for searchability.
- Coverage dates: `end_date >= start_date`; ACTIVE requires start ≤ today ≤ end (configurable grace).
- One ACTIVE policy per (farm, season, crop, product) among non-deleted — prevent double cover (configurable hard/soft).
- Currency ISO-4217; default `RWF`.
- `coverage_amount` and `premium_amount` on policy are **denormalized contract totals** consistent with accepted quote + premium rows.
- Renewal creates a new aggregate; never mutate historical premium amounts on the parent.

### Beneficiaries
- At least one beneficiary optional at DRAFT; required before ACTIVE if product flag `beneficiariesRequired=true`.
- Share percentages must sum to 100 when shares are used.

### Premiums
- Premium rows created at APPROVED → PREMIUM_PENDING.
- Status: `DUE` → `PAID` | `WAIVED` | `CANCELLED`.
- Activation requires all non-waived premiums `PAID` (Phase 5 may simulate payment confirmation via authorized action; real PSP later).

### Documents
- On ACTIVE: generate Certificate + Coverage Summary + Terms (version 1).
- Regenerations create new versions; prior versions immutable.
- QR encodes policy number + verification URL stub + content hash.
- Digital signature: store `signature_status=PENDING|SIGNED|N/A` and placeholder fields; no mandatory PKI in Phase 5.

### Suspension / cancellation
- Reason codes from configuration catalog + free-text.
- Cancelled/Expired policies are read-only except audit/document download.

---

## 8. Validation Rules

| Field / concern | Rule |
|-----------------|------|
| Policy number | Unique; format `POL-\d{4}-\d{8}` when system-generated |
| Farmer / farm | Exist, not deleted; ownership match |
| Product / package / type | ACTIVE/enabled |
| Season / crop | Exist; crop season consistent with farm when provided |
| Dates | end ≥ start; waiting period ≤ coverage length |
| Coverage amount | > 0; ≤ package max sum insured |
| Premium quote | Not expired (TTL configurable, default 7 days); factors hash matches recalculation unless override with reason |
| National portfolio | District derived from farm/farmer for search |
| Geometry readiness | Farm should have ACTIVE boundary for issuance (hard fail if product requires it) |
| Approver | Distinct from submitter when dual-control config enabled (default off in local) |

---

## 9. Pricing Strategy (Premium Engine)

### Principle

**Do not hardcode formulas in services.**  
Pricing is a **strategy pipeline** driven by `premium_pricing_rules` + product configuration.

### Components

```text
PremiumPricingContext
  farmerId, farmId, cropId, seasonId, productId, packageId,
  areaHa, riskZoneCode, coverageLevel, insuranceCompanyId,
  partnerId?, subsidyProgramCode?, asOfDate

PremiumPricingEngine.quote(context) → PremiumQuote
  1. Resolve PricingStrategy by product.pricingStrategyCode
  2. Load ordered PricingFactor providers
  3. Each factor contributes Adjustment (amount or rate) + audit trail entry
  4. Apply taxes / subsidies last per rule order
  5. Persist immutable PremiumQuote (breakdown JSON + factorsHash)
```

### Strategy interface (conceptual)

| Type | Role |
|------|------|
| `PricingStrategy` | Orchestrates factor chain for a product family (e.g. `AREA_MULTIPLIER`, `SUM_INSURED_RATE`) |
| `PricingFactor` | Pluggable contributor |
| `PremiumQuoteRepository` | Persistence of immutable quotes |

### Required factors (Phase 5)

| Factor code | Input | Behavior |
|-------------|-------|----------|
| `CROP_TYPE` | cropId | Rate table by crop |
| `FARM_AREA` | areaHa from farm/boundary | Per-hectare or tiered |
| `RISK_ZONE` | district/risk_scores | Zone multiplier |
| `HISTORICAL_WEATHER` | climate aggregates / risk_scores | Multiplier or loading (read-only) |
| `COVERAGE_LEVEL` | package coverage % | Linear or stepped |
| `INSURANCE_PRODUCT` | product base rate | Base premium |
| `GOVERNMENT_SUBSIDY` | subsidy program config | Subtract / cost-share |
| `DISCOUNT_RULES` | promo/loyalty config | Capped discount |
| `PARTNER_AGREEMENTS` | partnerId rate card | Partner override |
| `TAXES` | tax config | Add VAT/levy |

### Configuration storage

- Rule definitions in `premium_pricing_rules` (`rule_code`, `product_id?`, `priority`, `value_json`, `status`).
- Runtime overrides via `configurations` keys where appropriate.
- Every quote stores **full breakdown** so historical premiums remain explainable after rule edits.

### Explicit non-goals (Phase 5)

- Live actuarial model training / ML pricing.
- Real-time payment gateway capture (authorized **mark paid** / subsidy waive only).

---

## 10. Policy Issuance Workflow

Guided wizard (`PolicyIssuanceDraft`) + resulting `InsurancePolicy`:

```text
[1 Select Farmer]
    → [2 Select Farm]
        → [3 Choose Season]
            → [4 Choose Crop]
                → [5 Choose Product / Package]
                    → [6 Premium Calculation (quote)]
                        → [7 Review]
                            → [8 Approval]
                                → [9 Payment Pending]
                                    → [10 Policy Active + Documents]
```

| Step | Data | Skip? |
|------|------|-------|
| 1 Farmer | farmerId (search) | No |
| 2 Farm | farmId owned by farmer | No |
| 3 Season | seasonId | No |
| 4 Crop | cropId / cropSeasonId | No |
| 5 Product | productId, policyTypeId, coveragePackageId | No |
| 6 Quote | call pricing engine; lock quoteId | No |
| 7 Review | read-only summary + beneficiaries | No |
| 8 Approval | UNDER_REVIEW → APPROVED | Dual-control optional |
| 9 Payment | PREMIUM_PENDING → mark paid/waive | No for ACTIVE |
| 10 Active | documents generated | Auto on activate |

Draft save: `PUT /api/v1/policy-issuance-drafts/{id}`.  
Submit/approve/activate: dedicated transition endpoints (not free-form status PATCH).

---

## 11. Document Generation

| Document type | Content |
|---------------|---------|
| `POLICY_CERTIFICATE` | Parties, policy number, period, sum insured, premium, QR |
| `TERMS_AND_CONDITIONS` | Product T&Cs version pinned at issue |
| `COVERAGE_SUMMARY` | Limits, exclusions, waiting periods snapshot |

**Versioning:** `(policy_id, document_type, version)` unique.  
**QR:** payload `{policyNumber, version, contentSha256}`.  
**Signature readiness:** columns `signed_at`, `signer_dn`, `signature_status` — population optional.

Storage: metadata in `policy_documents`; bytes via Engagement `documents`/`attachments` or object-store key in Phase 5 local filesystem/DB blob (document strategy ADR if needed).

---

## 12. Search & Reporting

### Search filters

Policy number, farmer (name/code/NID), farm name/code, district, insurance company, season, crop, status, coverage type/product, date range.

All list APIs: pagination, sort, CSV export where noted.

### Reporting (read models / aggregated queries)

| Report | Description |
|--------|-------------|
| Active policies | count + sum insured + premium |
| Expired policies | period filter |
| Policies by district | group by farm/farmer district |
| Coverage by crop | sum insured / count |
| Premium revenue | paid premiums in period |
| Portfolio distribution | by product / company / status |
| Renewals | policies with successors in period |
| Cancellations | cancelled with reason breakdown |

Phase 5 implements **API + dashboard widgets** via SQL aggregations; dedicated warehouse deferred.

---

## 13. Application Services (planned)

| Service | Responsibility |
|---------|----------------|
| `InsuranceProductService` | Product CRUD |
| `PolicyTypeService` | Type CRUD |
| `CoveragePackageService` | Packages, limits, exclusions, waiting periods |
| `PremiumPricingEngine` | Strategy + factors → quote |
| `PolicyService` | Lifecycle transitions, search, renew, suspend, cancel |
| `BeneficiaryService` | Beneficiary management under policy |
| `PolicyDocumentService` | Generate/version/download |
| `PolicyIssuanceService` | Draft wizard + submit orchestration |
| `PolicyReportingService` | Portfolio reports |
| `InsuranceAuditHelper` | Structured audit writes |

Controllers remain thin: authz, DTO validation, service calls.

Permissions: reuse `policies:read` / `policies:write`; add `policies:approve` in seed if dual-control enabled.

---

## 14. API Surface (canonical)

| Method | Path | Purpose |
|--------|------|---------|
| CRUD | `/api/v1/insurance-products` | Products |
| CRUD | `/api/v1/policy-types` | Policy types |
| CRUD | `/api/v1/coverage-packages` | Packages (+ nested limits) |
| POST | `/api/v1/premiums/quote` | Calculate immutable quote |
| GET | `/api/v1/premium-quotes/{id}` | Fetch quote |
| CRUD | `/api/v1/policy-issuance-drafts` | Wizard drafts |
| POST | `/api/v1/policy-issuance-drafts/{id}/submit` | Create SUBMITTED policy |
| CRUD/search | `/api/v1/policies` | Policy list/detail |
| POST | `/api/v1/policies/{id}/approve` | Approve |
| POST | `/api/v1/policies/{id}/reject` | Reject |
| POST | `/api/v1/policies/{id}/mark-premium-paid` | Payment confirmation |
| POST | `/api/v1/policies/{id}/activate` | Activate + documents |
| POST | `/api/v1/policies/{id}/suspend` | Suspend |
| POST | `/api/v1/policies/{id}/reinstate` | Reinstate |
| POST | `/api/v1/policies/{id}/cancel` | Cancel |
| POST | `/api/v1/policies/{id}/renew` | Start renewal |
| GET | `/api/v1/policies/{id}/timeline` | Status history |
| CRUD | `/api/v1/policies/{id}/beneficiaries` | Beneficiaries |
| GET/POST | `/api/v1/policies/{id}/documents` | List / regenerate |
| GET | `/api/v1/policies/export` | CSV |
| GET | `/api/v1/insurance/reports/*` | Portfolio reports |

OpenAPI annotations required. Replace mock `PolicyController` responses.

---

## 15. Frontend IA

| Page | Route |
|------|-------|
| Insurance dashboard | `/insurance` |
| Product management | `/insurance/products` |
| Policy list / search | `/policies` |
| Policy details + timeline | `/policies/:id` |
| Policy wizard | `/policies/new` |
| Premium calculator | `/insurance/premium-calculator` |
| Policy documents | `/policies/:id/documents` |
| Renewal | `/policies/:id/renew` |
| Cancellation | action on detail |

Reuse Phase 4 enterprise table patterns (search, filters, sort, pagination, CSV, loading/error/empty, a11y, responsive). **No mock policy seed data.**

---

## 16. Future Extensibility

- Dual-control maker-checker with `policies:approve`
- External payment PSP webhooks → auto ACTIVE
- Parametric index triggers feeding Claims
- Multi-currency and multi-year policies
- Reinsurance cession records
- eSign / national PKI integration for certificates
- Actuarial sandbox strategies registered via SPI
- Offline agent issuance with later sync

---

## 17. Testing Strategy (post-approval)

| Layer | Focus |
|-------|-------|
| Unit | State machine guards; pricing factor math; validation |
| Pricing | Same context + rules → deterministic quote; rule change does not alter old quotes |
| Workflow | Draft → submit → approve → pay → activate |
| State machine | Illegal transitions rejected |
| Integration / API | Controllers + PostGIS/Testcontainers; document generation creates versions |
| Reporting | Aggregation correctness on seeded fixtures |

---

## 18. Quality Gate

- [x] Policy lifecycle complete (enforced transitions)
- [x] Premium engine configurable (no hardcoded product formulas)
- [x] State machine enforced in domain/application
- [x] Product / type / package management complete
- [x] Policy wizard complete
- [x] Documents generated (certificate, T&Cs, coverage summary + QR)
- [x] APIs documented (OpenAPI)
- [x] Tests passing
- [x] UI responsive; no mock policy data
- [x] Audits for create/premium/approve/renew/suspend/cancel/documents

**Implementation complete (0.6.0).** See `Phase5Deliverables.md`.  
**Do not start Claims Management until Phase 5 is accepted.**
