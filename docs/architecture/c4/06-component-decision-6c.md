# C4 — Component: Decision Engine (Stage 6C)

```text
[Operator UI]
    │  POST /tasks/{id}/decisions
    ▼
[WorkflowDecisionController]
    │
    ▼
[DecisionService]
    ├── DecisionValidator (allow-list, comment/target, graph edge)
    ├── DecisionRuleResolver (scoped decision_rules)
    ├── DecisionHistoryService
    ├── TaskAssignmentService (REASSIGN / DELEGATE)
    ├── WorkflowRuntime.transition (definition-driven routing)
    └── ApplicationEventPublisher → WorkflowDecisionRecordedEvent

Persistence: decision_types, decision_outcomes, decision_rules, workflow_decisions
Timeline: workflow_events(DECISION)
```

No Claims / Policy / Insurance types in this component.
