# C4 — Container Diagram (Level 2)

```text
┌──────────────────────────────────────────────────────────────────┐
│                         Web Browser                               │
│                    React SPA (Vite)                               │
│              (Insurance / Agri UI; Tasks later)                   │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTPS + HttpOnly cookies
┌────────────────────────────▼─────────────────────────────────────┐
│                 Spring Boot API (modular monolith)                │
│  IAM │ Agriculture │ Insurance │ Workflow (6A) │ …               │
└──────────────┬───────────────────────────────┬───────────────────┘
               │ JDBC                          │ (future adapters)
┌──────────────▼──────────────┐     ┌──────────▼──────────┐
│  PostgreSQL 16 + PostGIS    │     │ Notification / SMS  │
│  Liquibase migrations       │     │ (Stage 6D+)         │
└─────────────────────────────┘     └─────────────────────┘
```

**Workflow container boundary (logical):** packages under `*.workflow` + tables `workflow_*`. Same deployable as the rest of the platform (ADR-006).
