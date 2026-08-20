package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.workflow.WorkflowEventType;
import com.aegisterra.platform.domain.workflow.WorkflowGraph;
import com.aegisterra.platform.domain.workflow.WorkflowInstanceStatus;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowStepEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowStepRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTransitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTransitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Core runtime: start instance, validate/execute transitions, persist timeline events, create tasks.
 */
@Service
public class WorkflowRuntime {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowAuditHelper auditHelper;
    private final WorkflowTaskFactory taskFactory;
    private final EventBus eventBus;

    public WorkflowRuntime(
        WorkflowInstanceRepository instanceRepository,
        WorkflowStepRepository stepRepository,
        WorkflowTransitionRepository transitionRepository,
        WorkflowEventRepository eventRepository,
        ObjectMapper objectMapper,
        WorkflowAuditHelper auditHelper,
        WorkflowTaskFactory taskFactory,
        EventBus eventBus
    ) {
        this.instanceRepository = instanceRepository;
        this.stepRepository = stepRepository;
        this.transitionRepository = transitionRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.auditHelper = auditHelper;
        this.taskFactory = taskFactory;
        this.eventBus = eventBus;
    }

    @Transactional
    public WorkflowInstanceEntity start(
        UUID definitionId,
        WorkflowDefinitionVersionEntity publishedVersion,
        String subjectType,
        UUID subjectId,
        String correlationId,
        String payloadJson,
        UUID actorId
    ) {
        WorkflowGraph graph = WorkflowGraph.parse(publishedVersion.getGraphJson(), objectMapper);
        Instant now = Instant.now();

        WorkflowInstanceEntity instance = new WorkflowInstanceEntity();
        instance.setDefinitionId(definitionId);
        instance.setDefinitionVersionId(publishedVersion.getId());
        instance.setSubjectType(subjectType.trim().toUpperCase());
        instance.setSubjectId(subjectId);
        instance.setCurrentStepCode(graph.initialStep());
        instance.setCorrelationId(correlationId);
        instance.setPayloadJson(payloadJson);
        instance.setStartedAt(now);
        instance.setStatus(WorkflowInstanceStatus.RUNNING.name());
        instance.setDeleted(false);
        instance.setCreatedBy(actorId);
        instanceRepository.save(instance);

        WorkflowStepEntity step = enterStep(instance.getId(), graph.initialStep(), graph.stepName(graph.initialStep()), now, actorId);
        taskFactory.createForStep(instance, step, graph, actorId);

        appendEvent(instance.getId(), WorkflowEventType.CREATED, null, graph.initialStep(), null, actorId,
            "Workflow instance created", now);
        appendEvent(instance.getId(), WorkflowEventType.STARTED, null, graph.initialStep(), null, actorId,
            "Workflow started at " + graph.initialStep(), now);

        auditHelper.record(AuditAction.WORKFLOW_STARTED, actorId, "workflow_instance", instance.getId(),
            auditHelper.mapOf(
                "definitionId", definitionId,
                "definitionVersionId", publishedVersion.getId(),
                "versionNo", publishedVersion.getVersionNo(),
                "subjectType", instance.getSubjectType(),
                "subjectId", subjectId
            ));

        Map<String, Object> startedPayload = new HashMap<>();
        startedPayload.put("instanceId", instance.getId().toString());
        startedPayload.put("stepCode", graph.initialStep());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.WORKFLOW_STARTED,
            actorId,
            instance.getSubjectType(),
            instance.getSubjectId(),
            instance.getCorrelationId(),
            startedPayload
        ));
        return instance;
    }

    @Transactional
    public WorkflowInstanceEntity transition(
        WorkflowInstanceEntity instance,
        WorkflowDefinitionVersionEntity pinnedVersion,
        String action,
        String reason,
        UUID actorId
    ) {
        WorkflowInstanceStatus status = WorkflowInstanceStatus.parse(instance.getStatus());
        if (status.isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow instance is already terminal: " + status);
        }
        if (!pinnedVersion.getId().equals(instance.getDefinitionVersionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pinned definition version mismatch");
        }

        WorkflowGraph graph = WorkflowGraph.parse(pinnedVersion.getGraphJson(), objectMapper);
        String from = instance.getCurrentStepCode();
        graph.assertCanTransition(from, action);
        WorkflowGraph.TransitionEdge edge = graph.findTransition(from, action).orElseThrow();

        Instant now = Instant.now();
        taskFactory.cancelOpenTasksForStep(instance.getId(), from, actorId);
        exitActiveStep(instance.getId(), now, actorId);
        WorkflowStepEntity nextStep = enterStep(instance.getId(), edge.to(), graph.stepName(edge.to()), now, actorId);

        WorkflowTransitionEntity transition = new WorkflowTransitionEntity();
        transition.setInstanceId(instance.getId());
        transition.setFromStepCode(from);
        transition.setToStepCode(edge.to());
        transition.setActionCode(edge.action());
        transition.setActorId(actorId);
        transition.setReason(reason);
        transition.setOccurredAt(now);
        transition.setStatus("ACTIVE");
        transition.setDeleted(false);
        transition.setCreatedBy(actorId);
        transitionRepository.save(transition);

        instance.setCurrentStepCode(edge.to());
        instance.setUpdatedBy(actorId);

        appendEvent(instance.getId(), WorkflowEventType.TRANSITIONED, from, edge.to(), edge.action(), actorId,
            reason != null ? reason : "Transition " + edge.action(), now);

        if (graph.isTerminal(edge.to())) {
            WorkflowInstanceStatus terminal = resolveTerminalStatus(edge.to(), edge.action());
            instance.setStatus(terminal.name());
            instance.setCompletedAt(now);
            taskFactory.cancelOpenTasksForStep(instance.getId(), edge.to(), actorId);
            WorkflowEventType eventType = switch (terminal) {
                case REJECTED -> WorkflowEventType.REJECTED;
                case CANCELLED -> WorkflowEventType.CANCELLED;
                default -> WorkflowEventType.COMPLETED;
            };
            appendEvent(instance.getId(), eventType, from, edge.to(), edge.action(), actorId,
                "Workflow " + terminal.name().toLowerCase(), now);
            AuditAction auditAction = switch (terminal) {
                case REJECTED -> AuditAction.WORKFLOW_REJECTED;
                case CANCELLED -> AuditAction.WORKFLOW_CANCELLED;
                default -> AuditAction.WORKFLOW_COMPLETED;
            };
            auditHelper.record(auditAction, actorId, "workflow_instance", instance.getId(),
                auditHelper.mapOf("from", from, "to", edge.to(), "action", edge.action()));

            Map<String, Object> terminalPayload = new HashMap<>();
            terminalPayload.put("instanceId", instance.getId().toString());
            terminalPayload.put("terminalStatus", terminal.name());
            terminalPayload.put("stepCode", edge.to());
            terminalPayload.put("action", edge.action());
            String domainType = terminal == WorkflowInstanceStatus.COMPLETED
                ? DomainEventTypes.WORKFLOW_COMPLETED
                : DomainEventTypes.WORKFLOW_CANCELLED;
            eventBus.publish(PlatformDomainEvent.of(
                domainType,
                actorId,
                instance.getSubjectType(),
                instance.getSubjectId(),
                instance.getCorrelationId(),
                terminalPayload
            ));
        } else {
            instance.setStatus(WorkflowInstanceStatus.RUNNING.name());
            taskFactory.createForStep(instance, nextStep, graph, actorId);
            auditHelper.record(AuditAction.WORKFLOW_TRANSITIONED, actorId, "workflow_instance", instance.getId(),
                auditHelper.mapOf("from", from, "to", edge.to(), "action", edge.action()));
        }

        return instance;
    }

    private static WorkflowInstanceStatus resolveTerminalStatus(String stepCode, String action) {
        String step = stepCode.toUpperCase();
        String act = action.toUpperCase();
        if (act.equals("REJECT") || step.contains("REJECT")) {
            return WorkflowInstanceStatus.REJECTED;
        }
        if (act.equals("CANCEL") || step.contains("CANCEL")) {
            return WorkflowInstanceStatus.CANCELLED;
        }
        return WorkflowInstanceStatus.COMPLETED;
    }

    private WorkflowStepEntity enterStep(UUID instanceId, String code, String name, Instant at, UUID actorId) {
        WorkflowStepEntity step = new WorkflowStepEntity();
        step.setInstanceId(instanceId);
        step.setStepCode(code);
        step.setStepName(name);
        step.setEnteredAt(at);
        step.setStatus("ACTIVE");
        step.setDeleted(false);
        step.setCreatedBy(actorId);
        return stepRepository.save(step);
    }

    private void exitActiveStep(UUID instanceId, Instant at, UUID actorId) {
        stepRepository.findFirstByInstanceIdAndStatusAndDeletedFalseOrderByEnteredAtDesc(instanceId, "ACTIVE")
            .ifPresent(step -> {
                step.setStatus("COMPLETED");
                step.setExitedAt(at);
                step.setUpdatedBy(actorId);
            });
    }

    private void appendEvent(
        UUID instanceId,
        WorkflowEventType type,
        String from,
        String to,
        String action,
        UUID actorId,
        String message,
        Instant at
    ) {
        WorkflowEventEntity event = new WorkflowEventEntity();
        event.setInstanceId(instanceId);
        event.setEventType(type.name());
        event.setFromStepCode(from);
        event.setToStepCode(to);
        event.setActionCode(action);
        event.setActorId(actorId);
        event.setMessage(message);
        event.setOccurredAt(at);
        event.setStatus("ACTIVE");
        event.setDeleted(false);
        event.setCreatedBy(actorId);
        eventRepository.save(event);
    }
}
