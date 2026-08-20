# C4 — System Context (Level 1)

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                         AegisTerra Platform                              │
│         National agricultural insurance operations system                │
└───────────────┬─────────────────────────────┬───────────────────────────┘
                │                             │
     ┌──────────▼──────────┐       ┌──────────▼──────────┐
     │  Operators / Admin  │       │ Farmers (future     │
     │  Insurers / MoA     │       │ mobile / partners)  │
     └──────────┬──────────┘       └──────────┬──────────┘
                │                             │
                └────────────┬────────────────┘
                             │ HTTPS / SPA
                ┌────────────▼────────────┐
                │   AegisTerra Software   │
                │   System (this repo)    │
                └────────────┬────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
┌───────▼───────┐   ┌────────▼────────┐   ┌──────▼───────┐
│ PostgreSQL +  │   │ Future: SMS /   │   │ Future: PSP  │
│ PostGIS       │   │ Email / IDP     │   │ Banks / MoMo │
└───────────────┘   └─────────────────┘   └──────────────┘
```

**Stage 6A focus:** Operators manage generic workflow definitions and drive instances via API; no Claims consumer yet.
