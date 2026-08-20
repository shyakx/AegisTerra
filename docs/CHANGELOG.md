# Changelog

## [1.0.0-rc.2] - 2026-08-06
### Added — Phase 9 Enterprise Hardening (Release Candidate 2)
- Application contracts package: all API DTOs under `application.contracts` (presentation no longer owns contracts)
- Layer architecture fitness tests (`LayerArchitectureFitnessTest`)
- User admin: search/filter/pagination, disable/enable, audit history endpoint
- Profile self-service: `PUT /api/v1/auth/profile`
- Frontend Users page wired to live IAM APIs (roles, permissions catalog, audit)
- Settings: profile + password change; version display `1.0.0-RC2`
- Observability: actuator `health`/`info`/`metrics`, readiness/liveness probes, correlation ID in log pattern, Hibernate slow-query threshold (500ms), startup version log
- OpenAPI version sourced from `aegisterra.version`

### Changed
- Backend/frontend version `1.0.0-rc.2` (marketing label **1.0.0-RC2**)
- Health endpoint version no longer hardcoded
- Security headers: Permissions-Policy added
- RbacController uses `RbacQueryService` (no repository injection in presentation)

### Removed
- Scaffold weather-alert controller / dead anemic domain POJOs
- Application→presentation DTO dependency inversion

### Honest status / remaining debt
- External provider stubs (payment/climate) unchanged by design
- User search still filters in-memory (acceptable for operator-scale IAM)
- Full distributed tracing / log shipping deferred
- Advanced Finance & PSP reconciliation remain future roadmap (not started)

## [1.0.0-rc.1] - 2026-08-06
### Added — Version 1.0 Demo Candidate (Release Candidate)
- Liquibase `020-demo-seed-rc1`: realistic national demo portfolio (stations, observations, farmers/farms/boundaries, policies, claims, settlements, risk scores, alerts, district snapshots, notifications)
- Executive Overview API: `GET /api/v1/executive/overview`, `GET /api/v1/executive/regional-summary`
- Frontend: executive national dashboard (live KPIs), enterprise nav grouping, branded login, GIS layer controls, shared PageHeader/KpiCard/StatusBadge, CSV export on insurance + settlement reports
- Tests: `ExecutiveOverviewIntegrationTest`; LiquibaseMigrationIT asserts RC1 farmer seed

### Changed
- Backend/frontend version `1.0.0-rc.1`

## [0.14.0] - 2026-08-06
### Added — Phase 8A Climate Data Platform + Phase 8B Climate Intelligence
- Liquibase `018-climate-data-phase8a`: evolve stations/observations/satellite; providers, datasets, import jobs, validation results, quality reports, raster layers, aggregates; MANUAL + stub providers; `climate:*` / `reports:climate`
- Liquibase `019-climate-intelligence-phase8b`: evolve risk_scores / vegetation_indices; rule sets, alerts, farm profiles, indicator runs, district snapshots, intel jobs; `climate-intel:*` / `alerts:climate` / `reports:climate-intel`; seed `CLIMATE_RISK_V1`
- Climate Data SPI (`ClimateDataProvider`): Manual/CSV operational; National Met / OpenWeather / Tomorrow.io / NASA / Copernicus / Sentinel / Planet / NOAA stubs
- Ingest pipeline: normalize, validate, dual-write long+wide observations, quality reports, `ClimateImportCompleted` events
- Climate Intelligence: deterministic RiskEngine (weighted drought/flood/rainfall/vegetation), alerts, profiles, timelines, weather/season summaries, national/district dashboards, recalculation jobs; reads 8A only via `ClimateDataReadPort`
- APIs: `/api/v1/climate/**`, `/api/v1/climate-intel/**`
- Frontend: Climate Data UI (dashboard, stations, observations, imports, datasets, map) + Climate Intel UI (national risk, alerts, farm/district, recalc jobs); `/weather` redirects to `/climate`
- Tests: `ClimateDataPlatformIntegrationTest`, `ClimateIntelligencePlatformIntegrationTest`, `ClimateArchitectureFitnessTest`; LiquibaseMigrationIT extended

### Changed
- Backend/frontend version `0.14.0` (ships 8A+8B together; design targets were 0.13.0 / 0.14.0)

### Honest status / debt
- External climate provider HTTP integrations remain stubs; no partition detach/archive jobs yet; MapLibre tiles not wired (GeoJSON list UI); vegetation stress uses conservative default without band-math VI derivation; farm↔station spatial nearest-neighbor is first-station heuristic

## [0.12.0] - 2026-08-06
### Added — Phase 7 Enterprise Financial Settlement Platform
- Liquibase `017-settlement-phase7`: `settlements`, `settlement_status_history`, `settlement_workflow_links`, `ledger_transactions`, `ledger_entries`, `payment_provider_config`, `payment_provider_logs`, `settlement_batches`; permissions `settlements:*`, `ledger:read`, `reports:settlements`; seed `MANUAL` (+ stub providers), `SETTLEMENT_STANDARD` workflow, settlement notification templates
- Settlement aggregate + lifecycle; financial snapshot freeze; numbers `SET-{year}-{9 digits}`
- PaymentProvider SPI (`validate`/`authorize`/`execute`/`queryStatus`/`cancel`/`reverse`); **ManualSettlementProvider** operational; Bank/MoMo/Treasury/Partner stubs
- Immutable ledger (debit/credit; adjustments/reversals as new entries); `LedgerEntryCreated` events
- Intake from `ClaimApprovedForPayment`; Option A `ClaimsSettlementListener` (`PAYMENT_PENDING → SETTLED → CLOSURE_PENDING → CLOSED`)
- APIs: `/api/v1/settlements`, `/settlements/search`, `/settlements/reports`, `/ledger`, `/payment-providers`
- Frontend: settlement dashboard/list/details, finance review, ledger viewer, reports, provider config
- Tests: `SettlementPlatformIntegrationTest`, lifecycle/ledger/provider unit tests

### Changed
- Backend version `0.12.0`; ClaimStatus adds `CLOSURE_PENDING`; SettlementDomain status → Implemented

### Honest status / debt
- No real PSP integrations; no bank-statement reconciliation jobs; no FX engine; settlement workflow assignees seeded as `SYSTEM_ADMIN`; Workflow 6E parallel/SLA still deferred

## [0.11.0] - 2026-08-05
### Added — Phase 6F Enterprise Claims Management
- Liquibase `016-claims-phase6f`: extend `claims`; tables `claim_types`, `claim_type_document_rules`, `claim_drafts`, `claim_evidence`, `claim_inspections`, `claim_status_history`, `claim_decision_records`; extend `claim_assessments`; permissions `claims:assess|decide|admin`; seed claim types + `CLAIM_STANDARD` / `CLAIM_FAST_TRACK` workflows + claim notification templates
- Domain services: drafts/submit, evidence, assessment, inspection, coverage residual, lifecycle, `ClaimsWorkflowListener` (EventBus → domain status / `ClaimApprovedForPayment`)
- SPI stubs: fraud / weather / satellite verification ports
- APIs: `/api/v1/claims/**`, `/claim-types`, `/claims/reports/{name}`
- Frontend: claims dashboard, wizard, details, evidence/assessment/inspection; RBAC routes; reuse task inbox + workflow timeline
- Tests: `ClaimsManagementIntegrationTest`, `CoverageResidualServiceTest`, `ClaimLifecycleServiceTest`

### Changed
- Backend/frontend version `0.11.0`
- `ClaimsDomain.md` status → Implemented (v1 sequential)

### Honest status / debt
- No Workflow Stage 6E parallel/majority/SLA; claim workflow assignees seeded as `SYSTEM_ADMIN` for bootstrap; fraud/AI/satellite stubbed; evidence metadata-only (no blob store); Settlement not started

## [0.10.0] - 2026-08-05
### Added — Phase 6D Enterprise Communication & Event Platform
- Design: `docs/EventArchitecture.md`; WorkflowArchitecture §11 updated
- Liquibase `015-workflow-phase6d`: notification columns, templates, preferences, delivery log + seeds + `notifications:read|write`
- EventBus port + `SpringApplicationEventBus`; canonical `PlatformDomainEvent` envelope
- Workflow/task/decision publishers; Communication Engine (templates, preferences, IN_APP provider; EMAIL/SMS/PUSH/WEBHOOK stubs)
- APIs: `/api/v1/notifications*`, `/notification-preferences`
- Frontend: notification center, unread badge, detail, preferences
- Tests: `NotificationPlatformIntegrationTest`, `TemplateServiceTest`, `PlatformDomainEventTest`

### Changed
- Backend/frontend version `0.10.0`
- DecisionService publishes via EventBus (`DecisionRecorded`)

### Honest status
- No real SMTP/SMS/push/webhook providers; no outbox retry/dead-letter worker; no SLA/escalations or Claims consumer (6E+)

## [0.9.0] - 2026-08-05
### Added — Phase 6C Enterprise Decision Engine
- Liquibase `014-workflow-phase6c`: `decision_types`, `decision_outcomes`, `decision_rules`, `workflow_decisions` + seeded catalog + `decisions:read|act`
- Services: `DecisionService`, `DecisionValidator`, `DecisionHistoryService`, `DecisionRuleResolver`
- Domain event `WorkflowDecisionRecordedEvent`; timeline event type `DECISION`
- APIs: `GET /api/v1/decisions/types`, `POST/GET /api/v1/tasks/{id}/decisions`
- Frontend: decision dialog, decision history/timeline on task details
- Tests: `DecisionEngineIntegrationTest`, `DecisionEffectTest`
- Docs: WorkflowArchitecture §5A; Database/API updates

### Changed
- Backend/frontend version `0.9.0`
- Graph `task.allowedDecisions` optional allow-list for step-scoped decisions

### Honest status
- No notifications, SLA escalations, multi-task approval policies, or Claims consumer yet (6D+)

## [0.8.0] - 2026-08-05
### Added — Phase 6B Enterprise Task Engine
- Liquibase `013-workflow-phase6b`: `workflow_tasks`, `task_assignments`, `task_comments`, `task_attachments` + `tasks:read|act|assign`
- Task lifecycle, USER/ROLE assignment SPI, `WorkflowTaskFactory` on step entry
- APIs: `/api/v1/tasks`, `/my`, assign/claim/complete/cancel/comments
- Frontend: task inbox, my tasks, details, timeline component
- Tests: `TaskEngineIntegrationTest`
- Docs: WorkflowArchitecture §5 expanded; Database/API updates; C4 component note

### Changed
- Backend/frontend version `0.8.0`
- `WorkflowRuntime` creates/cancels tasks around transitions

### Honest status
- No approval policies, escalations, notifications, or Claims consumer yet (6C+)

## [0.7.0] - 2026-08-05
### Added — Phase 6A Workflow Engine Foundation
- Design accepted: `WorkflowArchitecture.md`, `ADR-008`
- Liquibase `012-workflow-phase6a`: definitions, definition versions, instances, steps, transitions, events + RBAC permissions
- Domain: `WorkflowGraph`, instance/version/event status types
- Application: `WorkflowDefinitionService`, `WorkflowInstanceService`, `WorkflowRuntime`
- REST: `/api/v1/workflows/definitions*`, `/api/v1/workflows/instances*`
- Tests: `WorkflowGraphTest`, `WorkflowEngineIntegrationTest` (versioning, illegal transition, timeline pin)
- Docs: `Database.md`, `API.md`, C4 under `docs/architecture/c4/`

### Changed
- Backend version `0.7.0`

### Honest status
- Stage 6A only — no tasks, approvals, escalations, notifications, or Claims consumer

## [0.6.0] - 2026-08-05
### Added — Phase 5 Insurance Core Domain
- Design: `docs/InsuranceDomain.md` (catalog, premium SPI, lifecycle, documents, APIs, UI, quality gate)
- Liquibase `011-insurance-phase5`: products, packages, limits/exclusions/waiting, pricing rules, quotes, policy columns, beneficiaries, documents, issuance drafts, templates, seed product + `policies:approve`
- Configuration-driven premium engine (`PricingStrategy` / `PricingFactor`) with `AREA_MULTIPLIER` and factor beans
- Policy aggregate + `PolicyStatus` state machine; issuance, documents (template render + QR/hash), search/export, reporting APIs
- Frontend: policies list/detail, issuance wizard, product catalog, premium calculator, insurance reports
- Tests: `PolicyStatusTest`, `PremiumPricingStrategyTest`, `InsuranceCoreIntegrationTest`
- Deliverables: `docs/Phase5Deliverables.md`

### Changed
- Backend/frontend version `0.6.0`
- Replaced mock policy controller responses with real insurance APIs

### Honest status
- Claims/inspections remain scaffolds until Phase 5 acceptance
- Document generation is template text (not branded PDF/PKI)
- Premium payment is operator-confirmed (no PSP)

## [0.5.0] - 2026-08-05
### Added — Phase 4 Agricultural Core Domain
- Design: `docs/FarmerDomain.md` (aggregates, rules, registration workflow, events)
- Liquibase `010-agriculture-phase4`: `farmer_code`, phone/email uniqueness, `registration_drafts`, boundary config keys, crop seeds
- Application services: Household, Farmer, Farm, Boundary, Plot, Crop, Season, CropSeason, Registration + Geometry + agri audit helper
- REST APIs under `/api/v1/{households,farmers,farms,farm-boundaries,plots,crops,seasons,crop-seasons,registration-drafts}` with OpenAPI + RBAC
- Search/pagination/CSV export for farmers and farms; geometry validate endpoint (PostGIS)
- Frontend: households, farmer list/profile, 8-step registration wizard (draft save/submit), farms, MapLibre boundary editor, plots, crops, seasons, crop history
- Tests: geometry service, agriculture registration IT, updated farmer/farm controller ITs

### Changed
- Backend/frontend version `0.5.0`
- Removed mock farmer/farm controller responses from production paths

### Honest status
- Insurance/policy/claims remain scaffolds (Phase 5+)
- Farm-boundary overlap detection is future-ready (TODO hook only)
- Admin boundary overlay GeoJSON API not yet wired
- Full KYC provider integration deferred

## [0.4.0] - 2026-08-05
### Added — Phase 3 Enterprise Data Platform
- Design docs: `DataArchitecture.md`, `ERD.md`, `PostGIS.md`, `RepositoryGuidelines.md`
- Docker Compose PostGIS (`postgis/postgis:16-3.4`) for local development
- Liquibase 003–009: PostGIS extension, geography, party, agriculture, insurance (+ history), climate, engagement/platform
- Hibernate Spatial + JTS geometry mappings (`MultiPolygon` / `Point`)
- JPA entities and Spring Data repositories under `infrastructure/persistence/{geography,party,agriculture,insurance,climate,engagement,platform}`
- Shared Testcontainers PostGIS utility; all `@SpringBootTest` classes use it
- Integration tests: `LiquibaseMigrationIT`, `FarmBoundaryRepositoryIT`, `FarmerRepositoryIT`

### Changed
- Default `local` profile uses PostgreSQL/PostGIS (`jdbc:postgresql://localhost:5432/aegisterra`)
- Optional `local-h2` profile retained for IAM-only quick starts (not for spatial work)
- Backend version `0.4.0`

### Honest status
- Persistence + spatial foundation only — no Farmer/Policy/Claim domain workflows or UI (Phase 4+)
- Mock presentation controllers for domain resources remain scaffolds
- History table writers/triggers deferred to later phases (structures ready)

## [0.3.0] - 2026-08-05
### Added — Phase 2 Identity & Security Platform
- Security architecture design: `docs/SecurityArchitecture.md`
- ADR-007: HttpOnly cookie token transport
- Liquibase IAM schema + role/permission seeds (UUID PKs, audit columns)
- Domain password policy; BCrypt strength 12
- JWT access tokens + opaque refresh tokens with rotation and family revoke-on-reuse
- Login sessions, password history, password reset tokens, security audit logs
- Auth APIs: login, refresh, logout, me, change-password, forgot-password, reset-password
- User admin APIs (CRUD) with `users:read` / `users:write`
- Roles and permissions list APIs
- RBAC method security (`@PreAuthorize`)
- Bootstrap SYSTEM_ADMIN user (local/test profiles)
- Login throttling and account lockout
- CORS + security headers baseline
- Frontend: AuthProvider, protected routes, cookie-based `apiClient`, forgot/reset password pages, permission-aware nav

### Changed
- Removed HTTP Basic and mock/scaffold token authentication
- Tokens no longer stored in `localStorage`
- OpenAPI documents bearer/cookie JWT era (0.3.0)

### Honest status
- Domain controllers (farmers/farms/policies/claims/…) remain scaffolds until Phases 4–7
- MFA challenge UX and external SSO are readiness-only (columns/hooks), not full product flows
- Email delivery for reset is logged stub in local/test
- Production must use PostgreSQL + strong `AEGISTERRA_JWT_SECRET` (H2 used for local/test velocity)

## [0.2.0] - 2026-08-05
### Added — Phase 1 Foundation
- Clean Architecture package skeletons (`application`, `infrastructure`)
- Config profiles: `local` (default), `test`, `prod`
- Demo credentials externalized and disabled by default / in `prod`
- Global API error model + `GlobalExceptionHandler`
- Correlation ID filter (`X-Correlation-Id` + MDC)
- OpenAPI baseline description (scaffold status called out)
- Actuator: health public; other actuator endpoints authenticated
- Frontend shared `apiClient`, Vite API proxy, `.env.example`
- ESLint + Prettier + `typecheck` scripts
- GitHub Actions CI (backend tests, frontend lint/typecheck/build)
- State ownership rule: TanStack Query for server state; Redux removed until justified

### Changed
- Login UI no longer prefills demo passwords
- `/auth/me` returns authenticated principal under HTTP Basic
- Scaffold login tokens labeled `SCAFFOLD` (not production JWT)
- Backend README aligned to Java 17 toolchain in `pom.xml`
- Trimmed unused frontend dependencies (maps, charts, i18n, Redux, etc.)

### Honest status
- Domain controllers remain mock scaffolds until Phase 3–7
- Real JWT (ADR-002) is **Phase 2**, not complete in 0.2.0

## [0.1.0] - 2026-08-05
### Added
- Initial architecture documentation baselines
- ADR-001..003 (later expanded; ADR-004..006 added in review)
- Backend/frontend scaffolds
- Controller smoke tests
