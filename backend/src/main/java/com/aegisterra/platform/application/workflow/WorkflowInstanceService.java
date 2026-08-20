package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTransitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTransitionRepository;
import com.aegisterra.platform.application.contracts.WorkflowEventResponse;
import com.aegisterra.platform.application.contracts.WorkflowInstanceResponse;
import com.aegisterra.platform.application.contracts.WorkflowStartRequest;
import com.aegisterra.platform.application.contracts.WorkflowTransitionLogResponse;
import com.aegisterra.platform.application.contracts.WorkflowTransitionRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowInstanceService {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowDefinitionVersionRepository versionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowEventRepository eventRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowRuntime runtime;

    public WorkflowInstanceService(
        WorkflowDefinitionService definitionService,
        WorkflowDefinitionVersionRepository versionRepository,
        WorkflowInstanceRepository instanceRepository,
        WorkflowEventRepository eventRepository,
        WorkflowTransitionRepository transitionRepository,
        WorkflowRuntime runtime
    ) {
        this.definitionService = definitionService;
        this.versionRepository = versionRepository;
        this.instanceRepository = instanceRepository;
        this.eventRepository = eventRepository;
        this.transitionRepository = transitionRepository;
        this.runtime = runtime;
    }

    @Transactional
    public WorkflowInstanceResponse start(WorkflowStartRequest request, UUID actorId) {
        WorkflowDefinitionEntity definition = definitionService.requireDefinition(request.definitionId());
        WorkflowDefinitionVersionEntity published = definitionService.requirePublishedVersion(definition);
        WorkflowInstanceEntity instance = runtime.start(
            definition.getId(),
            published,
            request.subjectType(),
            request.subjectId(),
            request.correlationId(),
            request.payloadJson(),
            actorId
        );
        return toResponse(instance, published.getVersionNo());
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse get(UUID id) {
        WorkflowInstanceEntity instance = require(id);
        int versionNo = versionRepository.findByIdAndDeletedFalse(instance.getDefinitionVersionId())
            .map(WorkflowDefinitionVersionEntity::getVersionNo)
            .orElse(0);
        return toResponse(instance, versionNo);
    }

    @Transactional
    public WorkflowInstanceResponse transition(UUID id, WorkflowTransitionRequest request, UUID actorId) {
        WorkflowInstanceEntity instance = require(id);
        WorkflowDefinitionVersionEntity pinned = versionRepository.findByIdAndDeletedFalse(instance.getDefinitionVersionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Pinned workflow version not found"));
        runtime.transition(instance, pinned, request.action(), request.reason(), actorId);
        return toResponse(instance, pinned.getVersionNo());
    }

    private WorkflowInstanceEntity require(UUID id) {
        return instanceRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
    }

    private WorkflowInstanceResponse toResponse(WorkflowInstanceEntity instance, int versionNo) {
        List<WorkflowEventResponse> events = eventRepository
            .findByInstanceIdAndDeletedFalseOrderByOccurredAtAsc(instance.getId())
            .stream()
            .map(this::toEvent)
            .toList();
        List<WorkflowTransitionLogResponse> transitions = transitionRepository
            .findByInstanceIdAndDeletedFalseOrderByOccurredAtAsc(instance.getId())
            .stream()
            .map(this::toTransition)
            .toList();
        return new WorkflowInstanceResponse(
            instance.getId(),
            instance.getDefinitionId(),
            instance.getDefinitionVersionId(),
            versionNo,
            instance.getSubjectType(),
            instance.getSubjectId(),
            instance.getCurrentStepCode(),
            instance.getStatus(),
            instance.getCorrelationId(),
            instance.getPayloadJson(),
            instance.getStartedAt(),
            instance.getCompletedAt(),
            events,
            transitions
        );
    }

    private WorkflowEventResponse toEvent(WorkflowEventEntity e) {
        return new WorkflowEventResponse(
            e.getId(), e.getEventType(), e.getFromStepCode(), e.getToStepCode(),
            e.getActionCode(), e.getActorId(), e.getMessage(), e.getOccurredAt()
        );
    }

    private WorkflowTransitionLogResponse toTransition(WorkflowTransitionEntity t) {
        return new WorkflowTransitionLogResponse(
            t.getId(), t.getFromStepCode(), t.getToStepCode(), t.getActionCode(),
            t.getActorId(), t.getReason(), t.getOccurredAt()
        );
    }
}
