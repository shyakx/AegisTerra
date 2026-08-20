# Database — Workflow Engine + Claims + Settlement + Climate

**Version:** 1.0.0-rc.2  
**Changelog:** `012`–`015` workflow/communication; `016` Claims; `017` Settlement; `018` Climate Data 8A; `019` Climate Intelligence 8B; `020` RC1 demo seed

## Tables

| Table | Purpose |
|-------|---------|
| `workflow_definitions` | Catalog entry (code, name); points at current `published_version_id` |
| `workflow_definition_versions` | Immutable graph snapshots |
| `workflow_instances` | Running/completed process; pins definition version |
| `workflow_steps` / `workflow_transitions` / `workflow_events` | Runtime tokens + timeline |
| `workflow_tasks` / `task_assignments` / `task_comments` / `task_attachments` | Task engine |
| `decision_types` / `decision_outcomes` / `decision_rules` / `workflow_decisions` | Decision engine |
| `notifications` | In-app inbox (extended in 015 with event/subject/template fields) |
| `notification_templates` | Channel templates with `{{placeholders}}` |
| `notification_preferences` | Per-user channel/event toggles |
| `notification_delivery_log` | Delivery attempts / failures |
| `claims` | Claim aggregate (extended in 016: type, snapshots, workflow binding, fraud, amounts) |
| `claim_types` / `claim_type_document_rules` | Claim catalog + required evidence matrix |
| `claim_drafts` | Wizard payloads before/around submit |
| `claim_evidence` | Evidence metadata (URI / GPS); no blob store in v1 |
| `claim_assessments` | Assessment cycles (extended columns in 016) |
| `claim_inspections` | Field/remote inspection records |
| `claim_status_history` | Domain status transitions |
| `claim_decision_records` | Domain mirror of adjudication outcomes |
| `settlements` | Settlement aggregate + frozen financial snapshot |
| `settlement_status_history` | Settlement status timeline |
| `settlement_workflow_links` | Settlement ↔ workflow instance |
| `ledger_transactions` / `ledger_entries` | Immutable accounting ledger |
| `payment_provider_config` / `payment_provider_logs` | Provider registry + execution audit |
| `settlement_batches` | Future mass-payment batches (schema only) |
| `climate_providers` / `climate_import_jobs` / `climate_datasets` | Climate Data catalog + ingest |
| `climate_validation_results` / `climate_quality_reports` | Ingest validation + quality |
| `climate_raster_layers` / `climate_observation_aggregates` | Raster catalog + rollups |
| `weather_stations` / `weather_observations` / `satellite_observations` | Evolved in 018 (long-format obs columns) |
| `climate_rule_sets` / `climate_alerts` / `farm_climate_profiles` | Climate Intelligence products |
| `climate_indicator_runs` / `district_risk_snapshots` / `climate_intel_jobs` | Indicators, district rollups, recalc jobs |
| `risk_scores` / `vegetation_indices` | Evolved in 019 under 8B ownership |

### RC1 demo seed (`020-demo-seed-rc1`)

Idempotent production-safe seed: extra stations + 14-day observations, 8 farmers / 12 farms / boundaries, 6 policies, 4 claims, 2 settlements, risk scores, open climate alerts, district risk snapshots, unread notifications (bound to admin at startup).

All tables: UUID PK, optimistic `version`, audit columns, FKs + indexes where applicable.

## Permissions seeded

`workflows:*`, `tasks:*`, `decisions:*`, `notifications:read|write`, `claims:read|write|assess|decide|admin`, `settlements:read|write|approve|process|admin`, `ledger:read`, `reports:settlements`, `climate:read|write|import|admin`, `reports:climate`, `climate-intel:read|write|admin`, `alerts:climate`, `reports:climate-intel` → SYSTEM_ADMIN (+ role grants per changelog).

## Out of scope (later stages)

Escalation scheduler, notification outbox/dead-letter worker, multi-task approval policy metadata, claim evidence blob storage, real PSP connectors, bank reconciliation jobs.
