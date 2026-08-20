# Phase 5 Deliverables — Insurance Core Domain

**Version:** 0.6.0  
**Status:** Implementation complete — awaiting stakeholder approval before Claims Management  
**Design:** `InsuranceDomain.md`

---

## 1. Architecture summary

Configuration-driven insurance engine on the existing Spring Boot / PostGIS stack:

| Layer | Responsibility |
|-------|----------------|
| Catalog | `insurance_products`, `policy_types`, `coverage_packages`, limits/exclusions/waiting periods |
| Pricing SPI | `PricingStrategy` + pluggable `PricingFactor` beans; rules in `premium_pricing_rules` |
| Policy aggregate | `insurance_policies` + beneficiaries; status only via `PolicyStatus` transitions |
| Documents | Template bodies in `configurations`; rendered into `policy_documents` with hash + QR |
| APIs | Thin controllers → application services → repositories; OpenAPI tags |
| UI | React pages under `/policies`, `/insurance/*` — no mock policy data |

Claims/payouts remain out of scope until Phase 5 acceptance.

---

## 2. Product engine summary

- Seeded product `CROP_MULTI_PERIL` (`AREA_MULTIPLIER`), type `MPCI_STANDARD`, package `PKG_80`
- Product config JSON holds currency / base rate; eligibility JSON (e.g. active boundary required)
- Coverage packages snapshot into `coverage_snapshot_json` at policy submit
- New products: insert catalog rows + pricing rules; add Java only when a new strategy/factor code is needed

---

## 3. Premium engine summary

Strategy: `AreaMultiplierPricingStrategy` (`AREA_MULTIPLIER`).

Configured factors (priority-ordered rules): crop, farm area (base×ha), risk zone, historical weather, coverage level, government subsidy, discounts, partner agreements, taxes.

Quotes persist to `premium_quotes` with breakdown JSON, factors hash, and TTL from `insurance.premium.quote_ttl_days`.

---

## 4. Policy lifecycle summary

`PolicyStatus` transition matrix validates every change. Illegal transitions → HTTP 409.

Typical happy path: submit → `UNDER_REVIEW` → approve → `PREMIUM_PENDING` → mark paid → `ACTIVE` (documents generated). Also: reject, suspend/reinstate, cancel, renew (new child policy).

No direct status column updates outside the aggregate service.

---

## 5. API summary

| Resource | Base path |
|----------|-----------|
| Products / types | `/api/v1/insurance-products`, `/api/v1/policy-types` |
| Packages | `/api/v1/coverage-packages` |
| Premiums | `POST /api/v1/premiums/quote`, `GET /api/v1/premium-quotes/{id}` |
| Policies | `/api/v1/policies` (+ search, export, transitions, documents) |
| Issuance drafts | `/api/v1/policy-issuance-drafts` |
| Reports | `/api/v1/insurance/reports/{active-policies\|expired-policies\|…}` |

RBAC: `policies:read|write|approve`. Swagger: `/swagger-ui.html`.

---

## 6. UI summary

| Page | Route |
|------|-------|
| Policy portfolio | `/policies` |
| Policy detail + actions/docs | `/policies/:id` |
| Issuance wizard | `/policies/issue` |
| Product catalog | `/insurance/products` |
| Premium calculator | `/insurance/calculator` |
| Reports | `/insurance/reports` |

---

## 7. Test summary

| Suite | Coverage |
|-------|----------|
| `PolicyStatusTest` | Transition matrix |
| `PremiumPricingStrategyTest` | Config-driven factor math |
| `InsuranceCoreIntegrationTest` | Catalog → quote → lifecycle → documents → report |
| `LiquibaseMigrationIT` | Schema incl. changelog 011 |
| Existing Phase 2–4 suite | Regression |

---

## 8. Technical debt (Phase 5-specific)

| Item | Notes |
|------|-------|
| PDF/PKI | Text templates + QR/hash; binary PDF branding and real digital signature deferred |
| Payment | Authorized “mark premium paid”; PSP integration later |
| Weather factor | Rule-driven multiplier; live climate series not wired |
| Users FE | Still mock (carried from earlier phases) |

---

**Stop here.** Do not start Claims Management until this phase is accepted.
