# AegisTerra Workflow Architecture

**Version:** 6.0.0 (Phase 6 design)  
**Status:** Approved — Stages 6A–6C delivered; Stage 6D Communication & Event Platform delivered (**0.10.0**)  
**Depends on:** Phase 2 IAM, Phase 3 data platform (engagement notifications scaffold), Phase 5 Insurance Core  
**First consumer (after engine acceptance):** Claims Management  
**Out of scope for engine delivery:** Claims domain rules, settlement/ledger, climate/AI, external BPMN runtime

This document is the **design gate** for Phase 6 — Enterprise Workflow & Business Process Engine.  
Related decision: `ADR/ADR-008-Reusable-Workflow-Engine.md` (**Accepted**).  
Related: `EventArchitecture.md` (Stage 6D).

### Stage delivery notes

| Stage | Status | Scope |
|-------|--------|-------|
| **6A** | Done (0.7.0) | Definitions, versioning, runtime transitions, `workflow_events` |
| **6B** | Done (0.8.0) | Task engine, assignment (USER/ROLE), inbox, comments/attachments metadata, FE inbox |
| **6C** | Done (0.9.0) | Decision Engine — configurable types/outcomes/rules, task decisions, definition-driven routing |
| **6D** | Done (0.10.0) | Event bus + Communication Engine (IN_APP); EMAIL/SMS/PUSH/WEBHOOK stubs |
| **6E** | Pending | SLA/escalations, approval policies (multi-task join) |
| **Claims design** | **Design gate ready** — `ClaimsDomain.md` (awaiting approval; no implementation) |
| **6F / 6G** | Pending | Seed `CLAIM_*` definitions + Claims consumer implementation |

---

## 1. Purpose

AegisTerra requires a **single, reusable workflow engine** that powers every multi-party approval and review process across the platform.

Business modules **must not** embed their own state machines, task queues, or approval graphs. They declare a **business subject** (e.g. Claim, Policy, Farmer KYC) and **start / advance / cancel** workflows through a published application API.

The engine must support national-scale operations:

| Stakeholder need | Engine capability |
|------------------|-------------------|
| Millions of farmers / high claim volume | Indexed task inbox, pagination, async notifications |
| Multiple insurers / ministries | Tenant-aware assignment rules (role, org, geography) |
| Dual control & audit | Immutable history + timeline; no silent status writes |
| Field + HQ collaboration | Tasks, comments, attachments, delegation |
| Future BPMN / Camunda | Definition model maps cleanly; runtime stays in-process first |

### Explicit non-goals (this phase)

- Embedding Camunda / Flowable / Temporal as the primary runtime (see ADR-008).
- Claims adjudication business rules (amount caps, fraud scores) — those remain in Claims domain.
- Settlement payments — Settlement Engine (later phase) consumes approved outcomes.
- Hard-coding claim steps inside Java `switch` statements in Claims services.

---

## 2. Architecture

### 2.1 Bounded context

| Context | Owns | Does not own |
|---------|------|--------------|
| **Workflow** | Definitions, instances, steps, transitions, tasks, assignments, escalations, comments, attachments metadata, history, notification *rules* | Claim amounts, policy premiums, KYC documents content validation |
| **Identity** | Users, roles, permissions used by assignment | Workflow graph |
| **Engagement** | Notification delivery channels, document blobs | When to notify (rules live in Workflow) |
| **Business domains** (Claims, Insurance, …) | Subject aggregates + domain invariants | Task lifecycle, approval majority math |

**Integration rule:** Domains reference workflows by `workflow_instance_id`. Workflows reference subjects by `(subject_type, subject_id)` only — never mutate subject tables except via **domain callbacks / application events** published after accepted transitions.

### 2.2 Layering (Clean Architecture)

```text
Presentation  → WorkflowController, TaskInboxController, DefinitionController
Application   → WorkflowRuntimeService, TaskService, ApprovalService,
                AssignmentService, EscalationService, NotificationDispatchService,
                TimelineQueryService
Domain        → WorkflowInstance aggregate, Task aggregate, Definition value model,
                TransitionGuard, ApprovalPolicy (SINGLE|SEQUENTIAL|PARALLEL|MAJORITY)
Infrastructure→ JPA entities/repos, Liquibase, clock/SLA scheduler, notification adapters
```

Package root (proposed): `com.aegisterra.platform.{domain,application,infrastructure,presentation}.workflow`

### 2.3 Runtime model

```text
WorkflowDefinition (versioned, config)
        │
        ▼ start(subjectType, subjectId, payload)
WorkflowInstance ──contains──► WorkflowStep (runtime copies / tokens)
        │                            │
        │                            ├── WorkflowTask(s)
        │                            ├── WorkflowAssignment
        │                            └── WorkflowComment / Attachment
        │
        ├── WorkflowTransition (attempted edges)
        ├── WorkflowHistory (append-only)
        └── WorkflowEscalation / Notification outbox rows
```

### 2.4 Eventing (in-monolith)

After each successful transition, the engine publishes an **application event** (Spring event first; transactional outbox ADR later if needed):

- `WorkflowStarted`
- `WorkflowTaskCreated` / `Assigned` / `Completed`
- `WorkflowStepCompleted`
- `WorkflowCompleted` / `Rejected` / `Cancelled`
- `WorkflowEscalated`
- `WorkflowNotificationRequested`

Business modules listen and update their aggregates (e.g. Claim status mirrors workflow outcome codes they registered).

### 2.5 Relationship to Phase 5 PolicyStatus

Policy lifecycle (`PolicyStatus`) remains the **Insurance domain contract state machine**. Phase 6 does **not** rewrite PolicyStatus.

Migration path (post-engine): optional **Policy Approval** definition that *drives* underwriting dual-control while PolicyStatus remains source of truth for financial contract state. Dual-running is allowed; Claims will be the first full consumer.

---

## 3. Workflow Definitions

A **WorkflowDefinition** is versioned configuration, not Java code.

| Field | Purpose |
|-------|---------|
| `code` | Stable key e.g. `CLAIM_STANDARD_V1` |
| `version` | Integer; instances pin to start-time version |
| `name` / `description` | Operator-facing |
| `subject_types` | Allowed subjects: `CLAIM`, `POLICY`, `FARMER`, `PAYOUT`, `KYC`, … |
| `graph_json` | Steps, transitions, guards, approval policy, SLA, assignment rules |
| `status` | `DRAFT` \| `PUBLISHED` \| `RETIRED` |
| `notification_profile_code` | Links to notification rule set |

### 3.1 Definition graph (JSON schema — conceptual)

```json
{
  "initialStep": "SUBMITTED",
  "terminalSteps": ["APPROVED", "REJECTED", "CANCELLED"],
  "steps": [
    {
      "code": "UNDER_REVIEW",
      "name": "Underwriting review",
      "taskType": "APPROVAL",
      "approvalPolicy": "SEQUENTIAL",
      "assignees": [{ "strategy": "ROLE", "value": "CLAIMS_ADJUSTER" }],
      "slaHours": 48,
      "escalation": { "afterHours": 48, "strategy": "ROLE", "value": "CLAIMS_SUPERVISOR" }
    }
  ],
  "transitions": [
    {
      "from": "SUBMITTED",
      "to": "UNDER_REVIEW",
      "action": "START_REVIEW",
      "guards": ["SUBJECT_ACTIVE_POLICY"]
    }
  ]
}
```

### 3.2 Definition lifecycle

`DRAFT` → `PUBLISHED` (immutable graph for that version) → `RETIRED`  
Starting an instance always uses a **published** version. Editing creates `version + 1` draft.

### 3.3 Seeded definitions (engine phase)

| Code | Purpose |
|------|---------|
| `CLAIM_STANDARD` | Standard agricultural claim path (used when Claims module lands) |
| `GENERIC_SINGLE_APPROVAL` | Smoke / reusable single approver |
| `GENERIC_SEQUENTIAL_APPROVAL` | Multi-level chain |
| `GENERIC_PARALLEL_MAJORITY` | Parallel + majority |

Claims-specific steps are configured in definition JSON; Claims code only starts/completes actions.

---

## 4. State Machine

### 4.1 Instance states

| State | Meaning |
|-------|---------|
| `CREATED` | Persisted, not started |
| `RUNNING` | Active token(s) on one or more steps |
| `WAITING` | Blocked on external signal / timer / parallel join |
| `COMPLETED` | Reached successful terminal step |
| `REJECTED` | Terminal rejection |
| `CANCELLED` | Aborted by authorized actor |
| `EXPIRED` | SLA / definition timeout policy |

### 4.2 Transition rules

Every transition must:

1. Load instance + definition version (pinned).
2. Validate action is allowed from current step token(s).
3. Evaluate **guards** (SPI: domain-provided or config predicates).
4. Enforce **approval policy** completion for the step (see §7).
5. Complete/cancel related tasks.
6. Move token(s); create next-step tasks.
7. Append `WorkflowHistory` + audit (`AuditAction`).
8. Enqueue notifications per rules.
9. Emit application event.

Illegal transitions fail with clear business exceptions (`409 CONFLICT` / `422 UNPROCESSABLE_ENTITY`).  
**No direct DB updates** to instance/step/task status outside the runtime service.

### 4.3 Guards (SPI)

```text
WorkflowGuard.evaluate(context) → Allow | Deny(reason)
```

Built-in: permission check, subject exists, not terminal.  
Domain-registered: e.g. Claims `ClaimWithinCoverageGuard`.

---

## 5. Task Engine

### 5.0 Stage 6B principles

- Tasks are **generic**: typed by `task_type` enum (`GENERIC`, `APPROVAL`, `VERIFICATION`, `INSPECTION`, `FINANCE`, …) — never by Claims/Policy Java types.
- Subject binding remains `(subject_type, subject_id)` on the **workflow instance**; tasks inherit subject context via `instance_id` (denormalized `subject_type`/`subject_id` columns for inbox query performance).
- `WorkflowTaskFactory` creates tasks when the runtime **enters** a step whose graph config includes `"task": { ... }`.
- Leaving a step (transition) **cancels** open tasks for that step that were not completed.
- Completing a task may optionally supply `advanceAction` to drive `WorkflowRuntime.transition` (human-driven progression).

### 5.1 Task lifecycle

```text
PENDING ──assign(USER)──► ASSIGNED ──claim/start──► IN_PROGRESS ──complete──► COMPLETED
   │                         │                          │
   │                         └──reassign────────────────┘
   │
   ├──claim (ROLE pool)──► IN_PROGRESS
   │
   ├──cancel──► CANCELLED
   ├──reject──► REJECTED
   └──(future SLA)──► EXPIRED / WAITING
```

| State | Meaning |
|-------|---------|
| `PENDING` | Created; role-pool or unassigned |
| `ASSIGNED` | Explicit user assignee |
| `IN_PROGRESS` | Claimed / actively worked |
| `WAITING` | Reserved for future dependency/SLA waits |
| `COMPLETED` | Successful outcome |
| `REJECTED` | Negative task outcome |
| `CANCELLED` | Superseded (step left / instance terminal) |
| `EXPIRED` | Reserved for future SLA stage |

### 5.2 Assignment model

| Strategy (6B) | Behavior |
|---------------|----------|
| `USER` | `assignee_user_id` set → `ASSIGNED` |
| `ROLE` | `assignee_role_code` set → `PENDING` pool; `claim` picks current user → `IN_PROGRESS` |

**Extension point:** `AssignmentStrategy` SPI — future `DEPARTMENT`, `REGION`/`GEOGRAPHY` without changing task aggregate.

**Ownership:** Assignee (user) or role pool. Reassignment appends `task_assignments` history (`ASSIGN`, `REASSIGN`, `CLAIM`).

**Completion:** Actor must be assignee (or `tasks:assign` override). Outcome + optional `advanceAction`.

### 5.3 Inbox model & querying

| Query | Semantics |
|-------|-----------|
| `/tasks` | Ops inbox — filter status, subjectType, taskType, assignee, role, q; paginate/sort |
| `/tasks/my` | Current user: assigned to me **or** PENDING in a role I hold |
| `/tasks/{id}` | Detail + comments + assignment history + instance timeline |

### 5.4 Task aggregate fields

| Field | Purpose |
|-------|---------|
| `task_type` | `GENERIC` \| `APPROVAL` \| `VERIFICATION` \| `INSPECTION` \| `FINANCE` |
| `title` / `description` | Operator-facing |
| `step_code` / `workflow_step_id` | Link to runtime step |
| `priority` | Ordering hint |
| `due_at` | Future SLA (nullable in 6B) |
| `payload_json` | Form hints |
| `outcome` / `completed_by` / `completed_at` | Completion audit |

### 5.5 Comments & attachments

- `task_comments` — threaded notes on a task  
- `task_attachments` — metadata + storage key / engagement document id (no Claims payloads)

### 5.6 Future SLA / approval integration

- **SLA (later):** scheduler flips overdue → `EXPIRED` / escalation; uses `due_at`.  
- **Approvals (later):** multiple tasks per step; join rules; task completion feeds approval policy — Task Engine stays generic.  
- **Decisions (6C):** human outcomes on tasks are recorded by the Decision Engine (§5A) and may advance the graph via definition edges — never via hardcoded Claims/Policy branches.

### 5.7 Graph task config (example)

```json
{
  "code": "REVIEW",
  "name": "Review",
  "task": {
    "enabled": true,
    "taskType": "GENERIC",
    "title": "Review workflow subject",
    "assignees": [{ "strategy": "ROLE", "value": "SYSTEM_ADMIN" }],
    "allowedDecisions": ["APPROVE", "REJECT", "REQUEST_INFORMATION", "DELEGATE"]
  }
}
```

---

## 5A. Decision Engine (Stage 6C)

### 5A.0 Principles

- Decisions are **generic catalog entries** (`decision_types`, `decision_outcomes`, `decision_rules`) — never Java enums hard-bound to Claims/Policy/Insurance.
- Every recorded decision belongs to a **task** (and inherits `subject_type` / `subject_id` from the workflow instance).
- Routing after a decision is **definition-driven**: resolved `workflow_action` must match a transition edge on the current step.
- The Task Engine remains unaware of business domains; `DecisionService` orchestrates validate → persist → effect → optional `WorkflowRuntime.transition`.

### 5A.1 Decision model

| Catalog | Purpose |
|---------|---------|
| `decision_types` | Operator-selectable codes (e.g. APPROVE, REJECT, RETURN, REQUEST_INFORMATION, ESCALATE, DELEGATE, AUTO_APPROVE, AUTO_REJECT, SKIP, CANCEL) |
| `decision_outcomes` | Normalized result labels applied to the task (`APPROVED`, `REJECTED`, …) |
| `decision_rules` | Maps type (+ optional definition/step/taskType scope) → outcome + effect + `workflow_action` |
| `workflow_decisions` | Immutable history of applied decisions |

Seeded types are **data**, not code constants. Operators may add types/rules later without redeploying domain modules.

### 5A.2 Decision lifecycle

```text
Request ──validate──► ResolvedRule ──persist workflow_decisions──► Apply effect
                                                                      │
                    ┌─────────────────────────────────────────────────┤
                    ▼                                                 ▼
            Complete / Reject / Cancel / Reassign / Keep open    (optional)
                    │
                    └── if effect advances ──► WorkflowRuntime.transition(workflow_action)
                    └── publish domain event + workflow_events(DECISION)
```

Invalid decisions are **rejected** (4xx) and not persisted.

### 5A.3 Decision types (examples — persisted seeds)

APPROVE · REJECT · RETURN · REQUEST_INFORMATION · ESCALATE · DELEGATE · AUTO_APPROVE · AUTO_REJECT · SKIP · CANCEL

Flags per type: `requires_comment`, `requires_target_user` (e.g. DELEGATE).

### 5A.4 Decision validation

`DecisionValidator` enforces:

1. Task is open; actor may act (assignee / role pool).
2. Decision type exists and is ACTIVE.
3. If step `task.allowedDecisions` is set, code must be listed.
4. Comment / target user when type flags require them.
5. A matching `decision_rules` row resolves (scoped then global).
6. If effect advances workflow, `workflow_action` must be a legal graph edge from the current step.

### 5A.5 Decision recording & history

`workflow_decisions` stores: task, instance, step, type, outcome, resolved action, effect, comment, target user, actor, timestamp, subject binding.  
`DecisionHistoryService` lists decisions for a task (newest last for timeline UX).

### 5A.6 Decision outcomes & effects

| Effect | Task | Workflow |
|--------|------|----------|
| `COMPLETE_AND_ADVANCE` | COMPLETED + outcome | `transition(workflow_action)` |
| `COMPLETE_ONLY` | COMPLETED + outcome | no advance |
| `REJECT_AND_ADVANCE` | REJECTED + outcome | `transition(workflow_action)` |
| `CANCEL_TASK` | CANCELLED + outcome | optional advance if rule sets action |
| `REASSIGN` | remains open; assignment updated | no advance |
| `KEEP_OPEN` | unchanged | optional advance (rare) |

### 5A.7 Decision routing

Routing is **not** hardcoded in the Decision Engine:

```text
Approve  → rule.workflow_action=APPROVE → edge REVIEW→NEXT
Reject   → rule.workflow_action=REJECT  → edge REVIEW→REJECTED
Request Information → REQUEST_INFORMATION → edge back to applicant step
Delegate → REASSIGN effect → TaskAssignmentService (no graph move unless rule says so)
```

Graph authors own the edges; rules only select which action code to fire.

### 5A.8 Future rules engine integration

Stage 6C uses table-driven `decision_rules` + optional step allow-lists. A future SpEL/DMN rules engine can replace `DecisionRuleResolver` behind the same port without changing task or Claims modules.

### 5A.9 Inbox / API surface (6C)

| API | Role |
|-----|------|
| `GET /api/v1/decisions/types` | Catalog for dialogs |
| `POST /api/v1/tasks/{id}/decisions` | Validate + execute |
| `GET /api/v1/tasks/{id}/decisions` | History |

---

## 6. Assignment Strategy

Assignment is rule-driven (definition + runtime overrides).

| Strategy | Behavior |
|----------|----------|
| `ROLE` | Assign to role pool; first claim wins or round-robin (config) |
| `USER` | Explicit user id |
| `ORG_UNIT` | Users in insurer / ministry org |
| `GEOGRAPHY` | District/sector matching subject location |
| `CREATOR_MANAGER` | Manager of initiating user |
| `PREVIOUS_ASSIGNEE` | Sticky assignment |
| `EXPRESSION` | Future: SpEL/JSONPath over subject snapshot |

**Candidate resolution** uses Identity permissions + optional org membership tables (extend IAM if missing — document as dependency).

**Reassignment:** authorized supervisor action; history records from→to.

**Delegation:** temporary assignee with expiry; original remains accountable in audit (“delegated by”).

---

## 7. Approval Engine

Configured per step via `approvalPolicy`:

| Policy | Semantics |
|--------|-----------|
| `SINGLE` | One completed APPROVE task advances |
| `SEQUENTIAL` | Ordered approver list; each level unlocks next |
| `PARALLEL` | N tasks created; all must APPROVE (AND-join) |
| `MAJORITY` | N tasks; advance when `ceil(N/2)+` APPROVE (ties configurable) |
| `ANY` | First APPROVE advances; others cancel |
| `AUTO_APPROVE` | Guard/timer auto-complete |
| `AUTO_REJECT` | Guard/timer auto-reject |

Supports:

- Multi-level chains (sequential policies across steps)
- Parallel branches (parallel gateway in graph → join step)
- Delegation / reassignment / escalation (see §8)
- SLA timeout → escalate or auto-reject/approve per step config

Rejection of a required approval typically transitions via defined `onReject` edge, not silent delete.

---

## 8. Escalation Strategy

| Trigger | Action options |
|---------|----------------|
| Task `due_at` passed | Notify; reassign to escalation role; raise priority; auto-reject/approve |
| Idle in `ASSIGNED` / `IN_PROGRESS` | Reminder notification |
| Workflow total SLA | Move to `EXPIRED` or escalate root |

Escalations are persisted (`WorkflowEscalation`) with reason, from assignee, to assignee/role, and linked history entry.

Scheduler: Spring scheduled job (cluster-safe via DB lease / `FOR UPDATE SKIP LOCKED` on due tasks). Redis lock may arrive in Production Readiness phase.

---

## 9. Sequential vs Parallel Workflow

### Sequential

Single active token. Transition completes step → next step. Matches classic claim review chains.

### Parallel

Definition may include `fork` / `join`:

- Fork creates multiple child tokens / task sets.
- Join step waits until policy satisfied (ALL / MAJORITY).
- Instance state may be `WAITING` until join.

Engine stores **tokens** in `workflow_step_instances` (or equivalent) with `parent_token_id` for nesting.

---

## 10. Delegation, Comments, Attachments

### Delegation

`POST /tasks/{id}/delegate` with `toUserId`, `until`, `reason`.  
Task remains same id; assignment history updated; notifications sent to both parties.

### Comments

`WorkflowComment`: body, author, visibility (`INTERNAL` \| `EXTERNAL`), linked to instance and optional task. Soft-delete with audit.

### Attachments

`WorkflowAttachment`: metadata + pointer to Engagement `documents`/`attachments` (or object storage key). Virus-scan hook reserved. Business modules may also attach on subject; workflow attachments are process evidence.

---

## 11. Notifications & Communication Engine

**Canonical design:** `EventArchitecture.md` (Stage 6D).

Business modules publish domain events only. The Communication Engine resolves channels, templates, and preferences.

| Channel | Stage 6D |
|---------|----------|
| `IN_APP` | Fully implemented → `notifications` table |
| `EMAIL` / `SMS` / `PUSH` / `WEBHOOK` | Provider interfaces + logging stubs |

Templates: `notification_templates` with `{{placeholders}}` — **no hardcoded email/SMS bodies in Java**.

Preferences: `notification_preferences` per user/channel/event type.

Delivery audit: `notification_delivery_log`. Retry/dead-letter workers are future (documented in EventArchitecture).

---

## 12. Audit Timeline

Every significant event appends to `workflow_history` and Identity audit log.

Timeline query returns ordered events for UI:

```text
Created → Submitted → Assigned → In Progress → Reviewed → Approved → Closed
```

Each node: timestamp, actor, action, from/to state, comment excerpt, correlation id.

Frontend: reusable `<WorkflowTimeline />` component (Claims, Policy approval, KYC share it).

---

## 13. Aggregates & schema (proposed Liquibase 012+)

| Table | Aggregate / purpose |
|-------|---------------------|
| `workflow_definitions` | Definition catalog + version + graph_json |
| `workflow_instances` | Instance root |
| `workflow_step_instances` | Active/completed step tokens |
| `workflow_tasks` | Task engine |
| `workflow_assignments` | Assignment history |
| `workflow_transitions` | Attempted/accepted transition log (optional if history covers) |
| `workflow_comments` | Comments |
| `workflow_attachments` | Attachment metadata |
| `workflow_history` | Append-only timeline |
| `decision_types` / `decision_outcomes` / `decision_rules` | Decision catalog (6C) |
| `workflow_decisions` | Decision records (6C) |
| `workflow_escalations` | Escalation records |
| `workflow_notification_rules` | Channel rules (or JSON in definitions) |
| `workflow_notification_outbox` | Reliable dispatch |

Permissions (seed): `workflows:read`, `workflows:admin`, `tasks:read`, `tasks:act`, plus domain permissions for starting subject workflows.

Reuse Phase 3 `notifications` for in-app delivery.

---

## 14. REST API surface (engine)

| Area | Paths (illustrative) |
|------|----------------------|
| Definitions | `GET/POST/PUT /api/v1/workflow-definitions`, publish |
| Instances | `POST /api/v1/workflow-instances`, `GET /{id}`, cancel |
| Actions | `POST /api/v1/workflow-instances/{id}/actions/{actionCode}` |
| Tasks | `GET /api/v1/tasks` (inbox), claim, complete, reject, reassign, delegate |
| Timeline | `GET /api/v1/workflow-instances/{id}/timeline` |
| Comments / attachments | nested under instance |
| Admin | escalation dry-run, definition validate |

All: validation, pagination, filtering, sorting, OpenAPI, consistent `ApiError`, RBAC.

---

## 15. Frontend

| Surface | Route (proposed) |
|---------|------------------|
| My tasks inbox | `/tasks` |
| Task detail | `/tasks/:id` |
| Workflow instance (admin/ops) | `/workflows/:id` |
| Definition browser (admin) | `/admin/workflow-definitions` |
| Shared timeline | component used by Claims/Policy pages |

UX requirements: loading/empty/error states, responsive, accessible controls, CSV export of inbox.

---

## 16. Future BPMN compatibility

| Now | Later |
|-----|-------|
| JSON graph with steps/transitions/gateways | Import BPMN 2.0 subset → same definition model |
| In-process Spring runtime | Optional Camunda/Flowable adapter behind `WorkflowRuntime` port |
| Spring application events | Outbox → Kafka/AMQP |

**Constraint:** Business modules depend only on **ports** (`WorkflowRuntime`, `TaskInbox`), never on BPMN engine APIs. Swapping runtime is an infrastructure change + ADR, not a Claims rewrite.

---

## 17. Claims as first consumer (after engine quality gate)

Claims Management **must**:

1. Own claim aggregate + financial/insurance invariants.
2. Call `workflowRuntime.start("CLAIM_STANDARD", CLAIM, claimId, …)`.
3. Map workflow terminal outcomes → claim domain status via listener.
4. Never implement parallel claim-only task tables for approval.

Claims design doc (`ClaimsDomain.md`) is the **Claims design gate**. It is authored after Stages 6A–6D; **implementation** waits for document approval and should not begin until the engine quality gate items required for the go-live claim types are accepted (prefer Stage 6E for high-value dual-control / SLA).

---

## 18. Testing strategy

| Layer | Coverage |
|-------|----------|
| Unit | Transition matrix, approval policies (majority/parallel), guard deny |
| Integration | Start → assign → complete → timeline; escalation job |
| API | Authz, validation, inbox pagination |
| Contract | Domain callback receives `WorkflowCompleted` |
| Frontend | Inbox + timeline smoke |

No mocks on production paths; test profiles may use stub email/SMS adapters.

---

## 19. Security

- All APIs authenticated; task act requires assignee **or** `tasks:act` override with audit.
- Definition publish requires `workflows:admin`.
- Subject-level authorization: engine asks domain `SubjectAccessPolicy.canAct(user, subject)`.
- Soft delete; history immutable.
- Attachment access inherits instance visibility rules.

---

## 20. Implementation plan (phased delivery)

| Stage | Deliverable | Exit |
|-------|-------------|------|
| **6A** | Liquibase + entities + definition CRUD + runtime start/transition | IT green |
| **6B** | Task engine + assignment + inbox API + UI | Operators can complete tasks |
| **6C** | Decision Engine (types/outcomes/rules + task decisions + routing) | Decisions configurable; history + FE |
| **6D** | Event bus + Communication Engine (IN_APP + provider SPI) | Events notify in-app; no direct sends |
| **6E** | SLA/escalations + approval policies (multi-task join) | Parallel/majority + due_at |
| **6F** | Seed `CLAIM_STANDARD` + Claims design gate | Ready for Claims consumer |
| **6G** | Claims Management implementation | Claims reuse engine only |

Stages 6A–6E = Workflow Engine quality gate.  
Claims consumer does **not** begin until the engine gate is accepted.

---

## 21. Quality gate

### Stage 6A (foundation) — delivered 0.7.0

- [x] Workflow definitions versioned and config-driven
- [x] Instance transitions enforced (no bypass updates)
- [x] Timeline events persisted (`workflow_events`)
- [x] OpenAPI + RBAC + tests passing
- [x] No business-specific (claims/insurance) workflow logic in engine
- [x] ADR-008 accepted

### Stage 6B (task engine) — delivered 0.8.0

- [x] Task engine operational with full open/terminal statuses
- [x] USER + ROLE assignment (+ SPI for future DEPARTMENT/REGION)
- [x] Claim / complete / cancel / inbox APIs + FE
- [x] No Claims-specific logic

### Stage 6C (decision engine) — delivered 0.9.0

- [x] Decision types/outcomes/rules persisted (seeded, not hardcoded)
- [x] Validate → record → effect → definition-driven routing
- [x] Decision history + domain event + workflow timeline
- [x] APIs + decision dialog UI
- [x] No Claims/Policy/Insurance-specific decision logic

### Stage 6D (communication & events) — delivered 0.10.0

- [x] EventBus port + Spring in-process transport
- [x] Workflow/task/decision events published from engine services
- [x] Communication Engine: templates, preferences, IN_APP delivery
- [x] EMAIL/SMS/PUSH/WEBHOOK provider interfaces (stubs)
- [x] Notification APIs + FE center/badge/preferences
- [x] No business module sends notifications directly

### Full Phase 6 engine (pending later stages)

- [ ] Single / sequential / parallel / majority approval
- [ ] SLA escalations
- [ ] Claims consumer

**Do not start Claims Management until the Workflow Engine quality gate is accepted.**  
**Do not start Stage 6E until Stage 6D is accepted.**

---

## 22. Roadmap realignment note

The prior roadmap labeled Phase 6 as “Claims & Inspections” only. Under the Enterprise Master Directive, Phase 6 is **Workflow Engine first**, with Claims as the first consumer. Subsequent settlement, climate, AI, and integrations remain later phases and must reuse this engine for any approval path.
