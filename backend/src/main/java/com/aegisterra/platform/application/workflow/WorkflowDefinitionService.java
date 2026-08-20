package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.workflow.WorkflowGraph;
import com.aegisterra.platform.domain.workflow.WorkflowVersionStatus;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionRepository;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionResponse;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionVersionRequest;
import com.aegisterra.platform.application.contracts.WorkflowDefinitionVersionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowDefinitionVersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowAuditHelper auditHelper;

    public WorkflowDefinitionService(
        WorkflowDefinitionRepository definitionRepository,
        WorkflowDefinitionVersionRepository versionRepository,
        ObjectMapper objectMapper,
        WorkflowAuditHelper auditHelper
    ) {
        this.definitionRepository = definitionRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> list() {
        return definitionRepository.findByDeletedFalseOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDefinitionResponse get(UUID id) {
        return toResponse(requireDefinition(id));
    }

    @Transactional
    public WorkflowDefinitionResponse create(WorkflowDefinitionRequest request, UUID actorId) {
        String code = request.code().trim().toUpperCase();
        definitionRepository.findByCodeAndDeletedFalse(code).ifPresent(x -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow definition code already exists");
        });
        WorkflowGraph.parse(request.graphJson(), objectMapper);

        WorkflowDefinitionEntity definition = new WorkflowDefinitionEntity();
        definition.setCode(code);
        definition.setName(request.name().trim());
        definition.setDescription(request.description());
        definition.setStatus("ACTIVE");
        definition.setDeleted(false);
        definition.setCreatedBy(actorId);
        definitionRepository.save(definition);

        WorkflowDefinitionVersionEntity version = newDraft(definition.getId(), 1, request.graphJson(), actorId);
        versionRepository.save(version);

        auditHelper.record(AuditAction.WORKFLOW_DEFINITION_CREATED, actorId, "workflow_definition", definition.getId(),
            auditHelper.mapOf("code", code, "versionNo", 1));
        return toResponse(definition);
    }

    /**
     * Creates an immutable-next draft version. Published graphs are never mutated in place.
     */
    @Transactional
    public WorkflowDefinitionVersionResponse createVersion(
        UUID definitionId,
        WorkflowDefinitionVersionRequest request,
        UUID actorId
    ) {
        WorkflowDefinitionEntity definition = requireDefinition(definitionId);
        WorkflowGraph.parse(request.graphJson(), objectMapper);

        versionRepository.findByDefinitionIdAndStatusAndDeletedFalse(definitionId, WorkflowVersionStatus.DRAFT.name())
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A DRAFT version already exists (id=" + existing.getId() + "). Publish or discard before creating another.");
            });

        int nextNo = versionRepository.findFirstByDefinitionIdAndDeletedFalseOrderByVersionNoDesc(definitionId)
            .map(v -> v.getVersionNo() + 1)
            .orElse(1);
        WorkflowDefinitionVersionEntity version = newDraft(definition.getId(), nextNo, request.graphJson(), actorId);
        versionRepository.save(version);
        definition.setUpdatedBy(actorId);

        auditHelper.record(AuditAction.WORKFLOW_VERSION_CREATED, actorId, "workflow_definition_version", version.getId(),
            auditHelper.mapOf("definitionId", definitionId, "versionNo", nextNo));
        return toVersionResponse(version);
    }

    @Transactional
    public WorkflowDefinitionResponse publish(UUID definitionId, UUID actorId) {
        WorkflowDefinitionEntity definition = requireDefinition(definitionId);
        WorkflowDefinitionVersionEntity draft = versionRepository
            .findByDefinitionIdAndStatusAndDeletedFalse(definitionId, WorkflowVersionStatus.DRAFT.name())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No DRAFT version to publish"));

        WorkflowGraph.parse(draft.getGraphJson(), objectMapper);

        if (definition.getPublishedVersionId() != null) {
            versionRepository.findByIdAndDeletedFalse(definition.getPublishedVersionId()).ifPresent(previous -> {
                previous.setStatus(WorkflowVersionStatus.RETIRED.name());
                previous.setUpdatedBy(actorId);
            });
        }

        Instant now = Instant.now();
        draft.setStatus(WorkflowVersionStatus.PUBLISHED.name());
        draft.setPublishedAt(now);
        draft.setPublishedBy(actorId);
        draft.setUpdatedBy(actorId);

        definition.setPublishedVersionId(draft.getId());
        definition.setUpdatedBy(actorId);

        auditHelper.record(AuditAction.WORKFLOW_DEFINITION_PUBLISHED, actorId, "workflow_definition", definitionId,
            auditHelper.mapOf("versionId", draft.getId(), "versionNo", draft.getVersionNo()));
        return toResponse(definition);
    }

    WorkflowDefinitionEntity requireDefinition(UUID id) {
        return definitionRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
    }

    WorkflowDefinitionVersionEntity requirePublishedVersion(WorkflowDefinitionEntity definition) {
        if (definition.getPublishedVersionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow definition has no published version");
        }
        WorkflowDefinitionVersionEntity version = versionRepository.findByIdAndDeletedFalse(definition.getPublishedVersionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Published version missing"));
        if (!WorkflowVersionStatus.PUBLISHED.name().equals(version.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Published version is not in PUBLISHED status");
        }
        return version;
    }

    private WorkflowDefinitionVersionEntity newDraft(UUID definitionId, int versionNo, String graphJson, UUID actorId) {
        WorkflowDefinitionVersionEntity version = new WorkflowDefinitionVersionEntity();
        version.setDefinitionId(definitionId);
        version.setVersionNo(versionNo);
        version.setGraphJson(graphJson);
        version.setStatus(WorkflowVersionStatus.DRAFT.name());
        version.setDeleted(false);
        version.setCreatedBy(actorId);
        return version;
    }

    private WorkflowDefinitionResponse toResponse(WorkflowDefinitionEntity definition) {
        List<WorkflowDefinitionVersionEntity> versions =
            versionRepository.findByDefinitionIdAndDeletedFalseOrderByVersionNoAsc(definition.getId());
        Integer publishedNo = versions.stream()
            .filter(v -> v.getId().equals(definition.getPublishedVersionId()))
            .map(WorkflowDefinitionVersionEntity::getVersionNo)
            .findFirst()
            .orElse(null);
        return new WorkflowDefinitionResponse(
            definition.getId(),
            definition.getCode(),
            definition.getName(),
            definition.getDescription(),
            definition.getPublishedVersionId(),
            publishedNo,
            definition.getStatus(),
            versions.stream().map(this::toVersionResponse).toList()
        );
    }

    private WorkflowDefinitionVersionResponse toVersionResponse(WorkflowDefinitionVersionEntity version) {
        return new WorkflowDefinitionVersionResponse(
            version.getId(),
            version.getDefinitionId(),
            version.getVersionNo(),
            version.getGraphJson(),
            version.getStatus(),
            version.getPublishedAt(),
            version.getPublishedBy()
        );
    }
}
