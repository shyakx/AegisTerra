# AegisTerra competition walkthrough

**Target duration:** 7–10 minutes
**Use case:** national agricultural insurance platform

## Operator accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@1234!Aa` | SYSTEM_ADMIN (full access) |
| `insurance.admin` | `Demo@1234!Aa` | INSURANCE_ADMIN |
| `insurance.officer` | `Demo@1234!Aa` | INSURANCE_OFFICER |
| `fi.officer` | `Demo@1234!Aa` | FI_OFFICER |
| `gov.analyst` | `Demo@1234!Aa` | GOVERNMENT_ANALYST |
| `aggregator` | `Demo@1234!Aa` | AGGREGATOR |
| `farmer.demo` | `Demo@1234!Aa` | FARMER — Jean Niyonzima (`FRM-2026-001`) |
| `auditor` | `Demo@1234!Aa` | AUDITOR |
| `support` | `Demo@1234!Aa` | SUPPORT |

> Type credentials on the sign-in screen. Navigation is permission-filtered per role.

## Walkthrough

| Step | Screen | Action | Expected result | What to say |
|---|---|---|---|---|
| 1 | Login | Sign in with `admin` / `Admin@1234!Aa` | Login succeeds and opens the dashboard | “This is the operational command view for the national platform.” |
| 2 | Executive dashboard | Show the KPI cards and climate alert summary | Farmers, farms, policies, claims, and alerts appear with live-looking values | “A single view brings the portfolio, claims, settlements, and climate posture together.” |
| 3 | Farmers | Open the farmers list and search `FRM-2026` | Farmer records appear, including Jean Niyonzima | “The farmer registry is the foundation for underwriting and policy lifecycle management.” |
| 4 | Farm details / GIS | Open a farm such as `FARM-2026-001` and show the boundary view | Farm details and GIS boundary render | “Each farm is linked to a boundary and a portfolio record, making spatial context part of the operational data model.” |
| 5 | Policies | Open the policies list and locate `POL-2026-001` | An active policy is visible with the expected coverage and premium | “This policy demonstrates how coverage, premium, and eligibility are captured in one place.” |
| 6 | Premium calculator | Open the insurance calculator and show the premium flow | The premium calculator page loads and shows the insured product context | “Premium logic is configurable and can be shown directly in the insurance workflow.” |
| 7 | Claims | Open the claims list and inspect `CLM-2026-001` | A claim with a real lifecycle status is visible | “The claim path shows how a loss event moves from submission through assessment and settlement.” |
| 8 | Workflow / tasks | Open the workflow/task area for the claim or settlement process | The workflow and task screens are available and reflect the current platform workflow model | “The platform uses governed workflow state rather than static status pages.” |
| 9 | Settlements | Open the settlement list and inspect `SET-2026-001` or `SET-2026-002` | Settlement records appear with completed and pending states | “Claims move into settlement and ledger handling under clear operational controls.” |
| 10 | Climate intelligence | Open climate intelligence and alert detail | Risk scores and alerts are visible | “Climate data becomes actionable risk intelligence for the operator.” |
| 11 | Role switch (optional) | Log out and sign in as `insurance.officer` or `fi.officer` / `Demo@1234!Aa` | Nav and actions shrink to that role’s permissions | “RBAC is live — each operator only sees what their mandate allows.” |
| 12 | Executive insight | Return as admin and point to the climate and operations posture | The executive overview gives a coherent national picture | “This is the business value: registration, insurance operations, settlement, and climate risk in one platform.” |

## Notes for the presenter

- Use the seeded codes: `FRM-2026`, `FARM-2026`, `POL-2026`, `CLM-2026`, `SET-2026`, `ALT-2026`.
- If a page is empty, stop and re-check startup rather than improvising.
- Keep the story grounded in the actual platform capabilities and seeded portfolio.
