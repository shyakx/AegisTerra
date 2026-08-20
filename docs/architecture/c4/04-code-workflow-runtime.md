# C4 — Code / Class Diagram — Workflow Runtime (Level 4)

```text
WorkflowDefinitionService
  + create(request, actorId)
  + createVersion(definitionId, graph, actorId)
  + publish(definitionId, actorId)
  + requirePublishedVersion(definition)

WorkflowInstanceService
  + start(request, actorId)
  + get(id)
  + transition(id, request, actorId)

WorkflowRuntime
  + start(definitionId, publishedVersion, subject*, actorId)
  + transition(instance, pinnedVersion, action, reason, actorId)

WorkflowGraph (value object)
  + parse(graphJson, mapper)
  + assertCanTransition(from, action)
  + findTransition(from, action)
  + isTerminal(step)

Entities: WorkflowDefinitionEntity, WorkflowDefinitionVersionEntity,
          WorkflowInstanceEntity, WorkflowStepEntity,
          WorkflowTransitionEntity, WorkflowEventEntity
```

**Invariant:** Instance always executes against `definition_version_id` pinned at start — never the catalog’s latest published pointer.
