# ADR-010: Product Focus — Climate & Yield Intelligence First

## Status

Accepted

## Date

2026-09-14

## Context

AegisTerra accumulated partner **insurance operations** surfaces (claims, settlements, ledger, policy admin, workflow/tasks, insured lending/inputs) alongside the climate–agriculture core. That breadth makes delivery harder and distracts from the owner’s stated need:

> A shared place where insurers and other stakeholders plan ahead from **past and current climate and yield behaviour**, so unexpected harvest-loss payouts are reduced and farmers/stakeholders see what is coming.

Business architecture already states AegisTerra is **climate risk intelligence**, not an insurance company. Climate Intelligence 8B already forbids embedding claims/ML in that bounded context.

## Decision

1. **Product north star** is the planning story:
   - Past climate + yield by place/crop
   - Recent change (e.g. last seasons → now)
   - Forward risk / expected outlook for planning
   - Shared views for insurers, farmers, banks, aggregators, government, development partners

2. **In product (active)**  
   Identity · Farmer/farm/crop/season registry · Geography/AEZ · Climate data · Climate intelligence (farm/district/AEZ/national) · Alerts · GIS · Notifications · Guidance · Planning outlook UI

3. **Out of product for now (frozen)**  
   Claims · Settlements/ledger/payment providers · Policy products/issuance/premium calculator · Workflow/task operator desks · Insured loans UI · Insured inputs UI · Satellite stub desk  
   These may remain in the repository and database history, but are **not** exposed in the default SPA navigation/routes and are **not** registered as HTTP APIs unless `aegisterra.modules.partner-ops=true`.

4. **Do not invent** spatial AEZ polygons, M:N zoning, BPMN adapters, or multi-tenant IdP work until the planning story works end-to-end.

5. **Partner ops code is technical debt / optional**, not the roadmap. Re-enabling requires an explicit product decision (new ADR or superseding this one).

## Consequences

### Positive

- Implementation and demos match owner language
- Less UI/API surface to secure, test, and explain
- Engineering time goes to historical climate + yield → outlook

### Negative / costs

- Existing partner-ops pages/APIs are dormant until re-enabled
- Integration tests that exercise ops APIs require `aegisterra.modules.partner-ops=true` (test profile)
- Liquibase history for ops tables is retained (do not rewrite migrations)

## Alternatives considered

| Alternative | Why not chosen |
|-------------|----------------|
| Delete all claims/settlement/workflow packages now | Breaks migration history, fitness tests, and optional re-enable; quarantine is safer |
| Keep ops in nav “for completeness” | Continues the complexity the owner rejected |
| ML forecasting platform immediately | Need a simple, explainable past→ahead slice first |
