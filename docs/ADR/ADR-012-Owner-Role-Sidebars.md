# ADR-012: Owner Role Sidebars & Scoped Partner Surfaces

## Status

Accepted

## Date

2026-09-21

## Context

The product owner specified **role-specific sidebars** (admin, insurance, bank, farmer, plus system roles) that reintroduce **Policies**, **Payouts**, partner **directories**, and **Agro-ecological zones**, while keeping climate/yield intelligence central. Admin also requires a dedicated **System intelligence** entry.

ADR-010 had frozen partner-ops UI. Owner guidance now requires those surfaces **scoped by role**, not a full claims desk.

## Decision

1. Navigation is **role catalogs** in `buildNavigation.ts` matching owner lists.
2. Admin sidebar includes: Dashboard, Climate hub, **System intelligence**, AEZ, Financial institutions, Insurance companies, Agricultural aggregators, Analytical reports, Payouts, Policies, Setting (+ Users for admins).
3. **System intelligence** = planning outlook + national/AEZ risk + alerts (intelligence plane), separate from Climate data ops.
4. Policies / Payouts / lending list pages are **re-exposed** for roles that need them. Claims/tasks desks stay redirected away.
5. Partner directories use curated partner lists (seeded front-end content). AEZ uses existing geography APIs.
6. Local profile enables `aegisterra.modules.partner-ops=true` so policy/settlement APIs register for live demos. Default `application.yml` may remain false for locked-down prod until ops decide.

## Consequences

- SPA matches owner login sketches.
- Partner-ops APIs are available again when the module flag is on.
- GIS / map desks remain frozen.

## Alternatives considered

| Alternative | Why not |
|-------------|---------|
| Keep ADR-010 freeze strictly | Conflicts with owner nav lists |
| Rebuild full insurance company UI | Out of scope; owner wants scoped access |
