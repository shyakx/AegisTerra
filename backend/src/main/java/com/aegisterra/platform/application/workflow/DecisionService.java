package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.workflow.DecisionEffect;
import com.aegisterra.platform.domain.workflow.WorkflowEventType;
import com.aegisterra.platform.domain.workflow.WorkflowGraph;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.workflow.DecisionTypeEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.DecisionTypeRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskRepository;
import com.aegisterra.platform.application.contracts.DecisionTypeResponse;
import com.aegisterra.platform.application.contracts.TaskDecisionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDecisionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DecisionService {

    private final DecisionTypeRepository typeRepository;
    private final WorkflowDecisionRepository decisionRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowDefinitionVersionRepository versionRepository;
    private final WorkflowEventRepository eventRepository;
    private final DecisionRuleResolver ruleResolver;
    private final DecisionValidator validator;
    private final DecisionHistoryService historyService;
    private final TaskAssignmentService assignmentService;
    private final WorkflowRuntime runtime;
    private final ObjectMapper objectMapper;
    private final WorkflowAuditHelper auditHelper;
    private final EventBus eventBus;

    public DecisionService(
        DecisionTypeRepository typeRepository,
        WorkflowDecisionRepository decisionRepository,
        WorkflowTaskRepository taskRepository,
        WorkflowInstanceRepository instanceRepository,
        WorkflowDefinitionRepository definitionRepository,
        WorkflowDefinitionVersionRepository versionRepository,
        WorkflowEventRepository eventRepository,
        DecisionRuleResolver ruleResolver,
        DecisionValidator validator,
        DecisionHistoryService historyService,
        TaskAssignmentService assignmentService,
        WorkflowRuntime runtime,
        ObjectMapper objectMapper,
        WorkflowAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.typeRepository = typeRepository;
        this.decisionRepository = decisionRepository;
        this.taskRepository = taskRepository;
        this.instanceRepository = instanceRepository;
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.eventRepository = eventRepository;
        this.ruleResolver = ruleResolver;
        this.validator = validator;
        this.historyService = historyService;
        this.assignmentService = assignmentService;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional(readOnly = true)
    public List<DecisionTypeResponse> listTypes() {
        return typeRepository.findByDeletedFalseAndStatusOrderBySortOrderAscCodeAsc("ACTIVE").stream()
            .map(t -> new DecisionTypeResponse(
                t.getCode(),
                t.getName(),
                t.getDescription(),
                t.isRequiresComment(),
                t.isRequiresTargetUser(),
                t.getSortOrder(),
                t.getStatus()
            ))
            .toList();
    }

    @Transactional
    public WorkflowDecisionResponse decide(UUID taskId, TaskDecisionRequest request, UUID actorId) {
        WorkflowTaskEntity task = taskRepository.findByIdAndDeletedFalse(taskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        String typeCode = request.decisionTypeCode().trim().toUpperCase();
        DecisionTypeEntity type = typeRepository.findByCodeAndDeletedFalse(typeCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown decision type: " + typeCode));

        WorkflowInstanceEntity instance = instanceRepository.findByIdAndDeletedFalse(task.getInstanceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow instance missing"));
        WorkflowDefinitionVersionEntity version = versionRepository.findByIdAndDeletedFalse(instance.getDefinitionVersionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Pinned version missing"));
        WorkflowDefinitionEntity definition = definitionRepository.findByIdAndDeletedFalse(instance.getDefinitionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow definition missing"));

        WorkflowGraph graph = WorkflowGraph.parse(version.getGraphJson(), objectMapper);
        DecisionRuleResolver.ResolvedRule rule = ruleResolver.resolve(
            typeCode,
            definition.getCode(),
            task.getStepCode(),
            task.getTaskType()
        );
        validator.validate(task, type, request, rule, graph, actorId);

        Instant now = Instant.now();
        WorkflowDecisionEntity decision = new WorkflowDecisionEntity();
        decision.setTaskId(task.getId());
        decision.setInstanceId(instance.getId());
        decision.setStepCode(task.getStepCode());
        decision.setDecisionTypeCode(typeCode);
        decision.setOutcomeCode(rule.outcomeCode());
        decision.setEffect(rule.effect().name());
        decision.setWorkflowAction(rule.workflowAction());
        decision.setCommentText(request.comment() == null ? null : request.comment().trim());
        decision.setTargetUserId(request.targetUserId());
        decision.setSubjectType(task.getSubjectType());
        decision.setSubjectId(task.getSubjectId());
        decision.setDecidedBy(actorId);
        decision.setDecidedAt(now);
        decision.setStatus("APPLIED");
        decision.setDeleted(false);
        decision.setCreatedBy(actorId);
        decisionRepository.save(decision);

        applyEffect(task, rule, request, actorId, now);

        appendDecisionEvent(instance.getId(), task.getStepCode(), typeCode, rule, actorId, now);
        auditHelper.record(AuditAction.WORKFLOW_DECISION_RECORDED, actorId, "workflow_decision", decision.getId(),
            auditHelper.mapOf(
                "taskId", taskId,
                "decisionType", typeCode,
                "outcome", rule.outcomeCode(),
                "effect", rule.effect().name(),
                "workflowAction", rule.workflowAction()
            ));

        if (shouldAdvance(rule)) {
            runtime.transition(instance, version, rule.workflowAction(), request.comment(), actorId);
        }

        Map<String, Object> decisionPayload = new HashMap<>();
        decisionPayload.put("decisionId", decision.getId().toString());
        decisionPayload.put("taskId", taskId.toString());
        decisionPayload.put("instanceId", instance.getId().toString());
        decisionPayload.put("decisionType", typeCode);
        decisionPayload.put("outcome", rule.outcomeCode());
        decisionPayload.put("effect", rule.effect().name());
        if (rule.workflowAction() != null) {
            decisionPayload.put("workflowAction", rule.workflowAction());
        }
        if (task.getAssigneeUserId() != null) {
            decisionPayload.put("assigneeUserId", task.getAssigneeUserId().toString());
        }
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.DECISION_RECORDED,
            actorId,
            task.getSubjectType(),
            task.getSubjectId(),
            instance.getCorrelationId(),
            decisionPayload
        ));

        return historyService.toResponse(decision);
    }

    private void applyEffect(
        WorkflowTaskEntity task,
        DecisionRuleResolver.ResolvedRule rule,
        TaskDecisionRequest request,
        UUID actorId,
        Instant now
    ) {
        DecisionEffect effect = rule.effect();
        switch (effect) {
            case COMPLETE_AND_ADVANCE, COMPLETE_ONLY -> {
                task.setStatus(WorkflowTaskStatus.COMPLETED.name());
                task.setOutcome(rule.outcomeCode());
                task.setCompletedAt(now);
                task.setCompletedBy(actorId);
                task.setUpdatedBy(actorId);
            }
            case REJECT_AND_ADVANCE -> {
                task.setStatus(WorkflowTaskStatus.REJECTED.name());
                task.setOutcome(rule.outcomeCode());
                task.setCompletedAt(now);
                task.setCompletedBy(actorId);
                task.setUpdatedBy(actorId);
            }
            case CANCEL_TASK -> {
                task.setStatus(WorkflowTaskStatus.CANCELLED.name());
                task.setOutcome(rule.outcomeCode());
                task.setUpdatedBy(actorId);
            }
            case REASSIGN -> {
                assignmentService.assignToUser(task, request.targetUserId(), actorId,
                    request.comment() == null ? "Delegated via decision" : request.comment());
                task.setUpdatedBy(actorId);
            }
            case KEEP_OPEN -> task.setUpdatedBy(actorId);
        }
    }

    private static boolean shouldAdvance(DecisionRuleResolver.ResolvedRule rule) {
        if (rule.workflowAction() == null || rule.workflowAction().isBlank()) {
            return false;
        }
        return rule.effect().advancesWorkflow()
            || rule.effect() == DecisionEffect.CANCEL_TASK
            || rule.effect() == DecisionEffect.KEEP_OPEN;
    }

    private void appendDecisionEvent(
        UUID instanceId,
        String stepCode,
        String typeCode,
        DecisionRuleResolver.ResolvedRule rule,
        UUID actorId,
        Instant at
    ) {
        WorkflowEventEntity event = new WorkflowEventEntity();
        event.setInstanceId(instanceId);
        event.setEventType(WorkflowEventType.DECISION.name());
        event.setFromStepCode(stepCode);
        event.setToStepCode(stepCode);
        event.setActionCode(typeCode);
        event.setActorId(actorId);
        event.setMessage("Decision " + typeCode + " → " + rule.outcomeCode()
            + (rule.workflowAction() == null ? "" : " (action " + rule.workflowAction() + ")"));
        event.setOccurredAt(at);
        event.setStatus("ACTIVE");
        event.setDeleted(false);
        event.setCreatedBy(actorId);
        eventRepository.save(event);
    }
}
