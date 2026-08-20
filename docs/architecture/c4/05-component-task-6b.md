# C4 — Component Diagram — Task Engine Stage 6B (Level 3)

```text
WorkflowRuntime
    │ enters step
    ▼
WorkflowTaskFactory ──► TaskAssignmentService ──► AssignmentStrategy (USER|ROLE)
    │
    ▼
workflow_tasks / task_assignments

WorkflowTaskController
    ├── TaskQueryService (inbox / my / detail+timeline)
    └── TaskService (assign, claim, complete±advance, cancel, comment)
```

Subject binding: tasks copy `subject_type` / `subject_id` from the instance for query performance — never Claims/Policy entity FKs.
