# ADR-008: Reusable In-Process Workflow Engine

## Status

Accepted

## Date

2026-08-05

## Context

AegisTerra will run many approval processes: claims, policy underwriting dual-control, KYC, payouts, subsidies, partner and compliance reviews. Embedding a bespoke state machine in each module duplicates logic, fragments audit timelines, and blocks national-scale operations tooling (unified task inbox, SLA, escalation).

Alternatives include adopting Camunda/Flowable/Temporal immediately, or continuing domain-local enums (as PolicyStatus does for the financial contract).

## Decision

1. Introduce a **first-class Workflow bounded context** inside the modular monolith.
2. Persist **versioned workflow definitions** (JSON graph) and run them with an **in-process Spring application runtime**.
3. Expose ports: `WorkflowRuntime`, task inbox, timeline; business modules depend only on these ports.
4. Deliver **Claims as the first consumer** only after the engine quality gate.
5. Keep Phase 5 `PolicyStatus` as Insurance’s financial contract state machine; optional policy-approval workflows may wrap dual-control later without replacing PolicyStatus semantics.
6. Design definition/runtime models for **future BPMN import** and optional external engine adapters — without requiring them now.

## Consequences

### Positive

- One task inbox, SLA, escalation, and timeline model for all domains
- Config-driven process changes without rewriting Claims/Insurance Java
- Clear path to Camunda/Flowable later behind the same ports
- Aligns with Clean Architecture and modular monolith (ADR-003, ADR-006)

### Negative / costs

- Upfront schema and runtime complexity before Claims UX lands
- Must carefully separate **domain invariants** (claim amounts) from **process state**
- Dual model briefly if Policy approval later adopts workflows alongside PolicyStatus

## Alternatives considered

| Alternative | Why not chosen (now) |
|-------------|----------------------|
| Camunda/Flowable day one | Ops/training overhead; overkill before process catalog stabilizes; still need AegisTerra task UX |
| Temporal / durable execution | Excellent for long-running integrations; heavier for human approval inbox; revisit for settlement retries |
| Domain-local state machines only | Guarantees duplication; no unified inbox; violates Master Directive |
| Rewrite PolicyStatus into workflows immediately | Unnecessary risk to accepted Insurance Core; PolicyStatus remains valid for contract lifecycle |

## Implementation note

See `docs/WorkflowArchitecture.md`. No workflow tables or services land until that design gate is approved.
