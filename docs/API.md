# API — Workflow Engine

**Base:** `/api/v1`  
**OpenAPI:** `/swagger-ui.html` (tags: Workflow Definitions, Workflow Instances, Workflow Tasks, Workflow Decisions)  
**Auth:** JWT HttpOnly cookies; RBAC permissions below.

## Definitions

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/workflows/definitions` | `workflows:read` | List |
| GET | `/workflows/definitions/{id}` | `workflows:read` | Includes versions |
| POST | `/workflows/definitions` | `workflows:admin` | Creates definition + DRAFT v1 |
| POST | `/workflows/definitions/{id}/versions` | `workflows:admin` | New DRAFT version |
| POST | `/workflows/definitions/{id}/publish` | `workflows:admin` | Publish current DRAFT |

### Create body

```json
{
  "code": "GENERIC_LINEAR",
  "name": "Generic linear",
  "description": "optional",
  "graphJson": "{ \"initialStep\": \"START\", \"terminalSteps\": [\"DONE\"], \"steps\": [...], \"transitions\": [...] }"
}
```

`graphJson` is a JSON **string** (escaped) or may be sent as stringified graph. Validation parses steps/transitions at create/publish.

## Instances

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| POST | `/workflows/instances` | `workflows:write` | Start on **published** version |
| GET | `/workflows/instances/{id}` | `workflows:read` | Includes events + transitions |
| POST | `/workflows/instances/{id}/transition` | `workflows:write` | `{ "action", "reason?" }` |

Illegal transitions → `409 CONFLICT`. Terminal instances reject further transitions.

## Tasks (Stage 6B)

| Method | Path | Permission |
|--------|------|------------|
| GET | `/tasks` | `tasks:read` |
| GET | `/tasks/my` | `tasks:read` |
| GET | `/tasks/{id}` | `tasks:read` |
| POST | `/tasks/{id}/assign` | `tasks:assign` |
| POST | `/tasks/{id}/claim` | `tasks:act` |
| POST | `/tasks/{id}/complete` | `tasks:act` |
| POST | `/tasks/{id}/cancel` | `tasks:act` / `tasks:assign` |
| POST | `/tasks/{id}/comments` | `tasks:act` |

Complete body may include optional `advanceAction` to drive `WorkflowRuntime.transition`. Prefer Decision Engine APIs when an outcome should be recorded.

## Decisions (Stage 6C)

| Method | Path | Permission |
|--------|------|------------|
| GET | `/decisions/types` | `decisions:read` / `tasks:read` |
| POST | `/tasks/{id}/decisions` | `decisions:act` / `tasks:act` |
| GET | `/tasks/{id}/decisions` | `decisions:read` / `tasks:read` |

### Record decision body

```json
{
  "decisionTypeCode": "APPROVE",
  "comment": "optional / required per type",
  "targetUserId": "required when type requires target user (e.g. DELEGATE)"
}
```

Routing uses `decision_rules.workflow_action` against the published graph — not hardcoded Claims/Policy branches. Invalid decisions → `422` and are not persisted.

## Notifications (Stage 6D)

| Method | Path | Permission |
|--------|------|------------|
| GET | `/notifications` | `notifications:read` |
| GET | `/notifications/unread` | `notifications:read` |
| GET | `/notifications/unread-count` | `notifications:read` |
| GET | `/notifications/{id}` | `notifications:read` |
| POST | `/notifications/{id}/read` | `notifications:write` |
| GET | `/notification-preferences` | `notifications:read` |
| PUT | `/notification-preferences` | `notifications:write` |

In-app rows are created by the Communication Engine from domain events — not by business controllers.

## Claims (Phase 6F)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/claims` | `claims:read` | Search/page (`q`, `status`, `claimTypeCode`, `policyId`, `farmerId`, dates) |
| GET | `/claims/{id}` | `claims:read` | Detail |
| POST | `/claims` | `claims:write` | Create **DRAFT** intake |
| POST | `/claims/{id}/submit` | `claims:write` | Validate evidence + start `CLAIM_*` workflow |
| POST | `/claims/{id}/cancel` | `claims:write` | Domain cancel |
| GET | `/claims/{id}/timeline` | `claims:read` | Status history |
| GET/POST | `/claims/{id}/evidence` | read/write | Evidence metadata |
| DELETE | `/claims/{id}/evidence/{evidenceId}` | `claims:write` | Soft-delete |
| GET/POST | `/claims/{id}/assessments` | read / `claims:assess` | Assessments |
| GET/POST | `/claims/{id}/inspections` | read / `claims:assess` | Inspections |
| POST | `/claims/drafts` · PUT · `/submit` | `claims:write` | Wizard drafts |
| GET | `/claim-types` | `claims:read` | Catalog |
| GET | `/claims/reports/{name}` | `claims:read` | `by-status`, `by-type` |

Adjudication stays on `/tasks/**` + decision APIs (`subjectType=CLAIM`).

## Settlements (Phase 7 / 0.12.0)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/settlements` | `settlements:read` | Search/page |
| POST | `/settlements/search` | `settlements:read` | Body filters |
| GET | `/settlements/{id}` | `settlements:read` | Detail |
| GET | `/settlements/{id}/timeline` | `settlements:read` | Status history |
| GET | `/settlements/{id}/ledger` | `settlements:read` / `ledger:read` | Ledger lines |
| POST | `/settlements` | `settlements:write` | Manual create + start `SETTLEMENT_STANDARD` |
| POST | `/settlements/{id}/manual-confirm` | `settlements:process` | ManualSettlementProvider confirm |
| POST | `/settlements/{id}/cancel` | `settlements:write` | Cancel (query `reason`) |
| GET | `/settlements/reports` | `reports:settlements` | Dashboard summary |
| GET | `/settlements/reports/{name}` | `reports:settlements` | `pending`, `completed`, `failed`, `by-provider`, `by-status`, `by-source`, `by-district`, `by-crop`, `by-company`, `avg-cycle-time` |
| GET | `/ledger` | `ledger:read` | Ledger transactions |
| GET | `/ledger/{id}` | `ledger:read` | Transaction + entries |
| GET | `/payment-providers` | `settlements:read` | Provider config (Manual enabled) |

Settlement finance review uses `/tasks/**` with `subjectType=SETTLEMENT`. Claims close only via `SettlementCompleted` → `ClaimsSettlementListener`.

## Climate Data (Phase 8A / 0.14.0)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/climate/dashboard` | `climate:read` | Job/obs/provider summary |
| GET | `/climate/providers` | `climate:read` | Catalog (MANUAL enabled; others stubs) |
| GET/POST | `/climate/stations` | read / `climate:write` | Register/search stations |
| GET | `/climate/stations/{id}` | `climate:read` | Detail |
| GET | `/climate/observations` | `climate:read` | **Requires** `from` + `to` |
| GET | `/climate/satellite-observations` | `climate:read` | EO search (window required) |
| GET | `/climate/raster-layers` | `climate:read` | Raster catalog |
| GET | `/climate/datasets` · `/{id}` | `climate:read` | Dataset browser |
| GET/POST | `/climate/import-jobs` | read / `climate:import` | Manual/CSV import |
| GET | `/climate/import-jobs/{id}` | `climate:read` | Job status |
| GET | `/climate/quality-reports` | `climate:read` | Quality rollups |
| GET | `/climate/map/stations` · `/map/footprints` | `climate:read` | GeoJSON |
| GET | `/climate/reports/{name}` | `reports:climate` | Named reports |

## Climate Intelligence (Phase 8B / 0.14.0)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/climate-intel/national/dashboard` | `climate-intel:read` | National risk snapshot |
| GET | `/climate-intel/farms/{farmId}/risk-score` | `climate-intel:read` | Latest farm score |
| GET | `/climate-intel/farms/{farmId}/profile` | `climate-intel:read` | Historical profile |
| GET | `/climate-intel/farms/{farmId}/timeline` | `climate-intel:read` | Alerts + indicators |
| GET | `/climate-intel/farms/{farmId}/weather-summary` | `climate-intel:read` | Requires `from`/`to` |
| GET | `/climate-intel/farms/{farmId}/season-summary` | `climate-intel:read` | Season card |
| GET | `/climate-intel/districts/{code}/risk-profile` | `climate-intel:read` | District rollup |
| GET | `/climate-intel/alerts` · `/{id}` | `climate-intel:read` / `alerts:climate` | Alert inbox |
| POST | `/climate-intel/alerts/{id}/acknowledge` | write / `alerts:climate` | Ack |
| POST | `/climate-intel/jobs/recalculate` | `climate-intel:admin` / write | Deterministic recalc |
| GET | `/climate-intel/indicators` | `climate-intel:read` | Indicator runs |
| GET | `/climate-intel/map/risk` | `climate-intel:read` | GeoJSON risk layer |
| GET | `/climate-intel/reports/{name}` | `reports:climate-intel` | Named reports |

8B never calls external climate providers. Claims/Insurance do not call Climate SPI.

## Executive Overview (RC 1.0 / 1.0.0-rc.1+)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/executive/overview` | any of `farmers:read`, `policies:read`, `claims:read`, `settlements:read`, `climate-intel:read`, `climate:read` | National KPI snapshot (farmers, farms, policies, claims, settlements, climate, tasks, notifications, regional, quick links) |
| GET | `/executive/regional-summary` | same | `{ "districts": [ { districtCode, meanScore, grade, farmCount } ] }` |

## Users & RBAC (RC2 / 1.0.0-rc.2)

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| GET | `/users` | `users:read` | Paged search (`q`, `status`, `page`, `size`) → `PageResponse` |
| GET | `/users/{id}` | `users:read` | Detail |
| GET | `/users/{id}/audit` | `users:read` | Last 50 audit events for resource `user` |
| POST | `/users` | `users:write` | Create (`roles` required) |
| PUT | `/users/{id}` | `users:write` | Update email/displayName/status/roles |
| POST | `/users/{id}/disable` · `/enable` | `users:write` | Status shortcuts |
| DELETE | `/users/{id}` | `users:write` | Soft delete |
| GET | `/roles` | `roles:read` | Role catalog |
| GET | `/permissions` | `permissions:read` | Permission catalog |
| PUT | `/auth/profile` | authenticated | Self-service email/displayName |

## Later-stage non-APIs

Escalations, approval-policy admin — Stage 6E+. Real PSP / treasury APIs — later. Live meteo/EO provider HTTP — post-8A stubs.
