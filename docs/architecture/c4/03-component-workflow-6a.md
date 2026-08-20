# C4 — Component Diagram — Workflow Engine Stage 6A (Level 3)

```text
┌──────────────────────────────── Presentation ────────────────────────────────┐
│  WorkflowDefinitionController     WorkflowInstanceController                 │
│  /api/v1/workflows/definitions*   /api/v1/workflows/instances*               │
└─────────────────────────────┬───────────────────────┬────────────────────────┘
                              │                       │
┌─────────────────────────────▼───────────────────────▼────────────────────────┐
│                           Application                                         │
│  WorkflowDefinitionService   WorkflowInstanceService   WorkflowRuntime       │
│  WorkflowAuditHelper                                                          │
└──────────────┬──────────────────────────┬───────────────────┬────────────────┘
               │                          │                   │
┌──────────────▼──────────┐  ┌────────────▼────────┐  ┌───────▼──────────────┐
│ Domain                  │  │ Persistence         │  │ Identity Audit       │
│ WorkflowGraph           │  │ *Repository / Entity│  │ AuditService         │
│ WorkflowInstanceStatus  │  │ workflow_* tables   │  └──────────────────────┘
│ WorkflowVersionStatus   │  └─────────────────────┘
│ WorkflowEventType       │
└─────────────────────────┘
```

**Not in 6A:** TaskService, ApprovalService, EscalationService, NotificationDispatchService.
