# Product focus (ADR-010 + ADR-012)

AegisTerra’s north star remains **climate and yield intelligence for planning**.  
ADR-012 adds **owner role sidebars** that re-expose scoped Policies, Payouts, partner directories, and AEZ views.

## Owner need

Insurers and other stakeholders rely on one place to:

1. Learn from **past** climate and yield behaviour  
2. See **current** risk  
3. Plan for what is **coming** — so harvest-loss payouts are less unexpected  

## Active product (by role)

- **System admin:** Dashboard, Climate hub, System intelligence, AEZ, partner directories, analytical reports, Payouts, Policies, Setting  
- **Insurance:** AEZ, Payouts notification, Policy, Setting  
- **Bank:** Farmers & loans, Payouts triggered, Policy, Setting  
- **Farmer:** Farmer status, Payouts, Setting  
- **Aggregator / Government / Auditor:** Climate, AEZ, reports (and read-only ops where permitted)

## Still frozen

GIS / live maps, claims desks, workflow/task desks as primary product.

## Planning depth

- Yield outlook + CSV (`035`)  
- ML Stage A maize spike (`036`, ADR-011) — experimental  
- Seed backfill (`037`)  
- Owner AEZ farmer registry AGT-0001–0450 in Postgres (`038`) + SPA table