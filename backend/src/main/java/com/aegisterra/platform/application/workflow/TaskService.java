package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskCommentEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskCommentRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionVersionRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskRepository;
import com.aegisterra.platform.application.contracts.TaskAssignRequest;
import com.aegisterra.platform.application.contracts.TaskCancelRequest;
import com.aegisterra.platform.application.contracts.TaskCommentRequest;
import com.aegisterra.platform.application.contracts.TaskCompleteRequest;
import com.aegisterra.platform.application.contracts.WorkflowTaskResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final WorkflowTaskRepository taskRepository;
    private final TaskAssignmentService assignmentService;
    private final TaskQueryService queryService;
    private final TaskCommentRepository commentRepository;
    private final RoleRepository roleRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowDefinitionVersionRepository versionRepository;
    private final WorkflowRuntime runtime;
    private final WorkflowAuditHelper auditHelper;
    private final EventBus eventBus;

    public TaskService(
        WorkflowTaskRepository taskRepository,
        TaskAssignmentService assignmentService,
        TaskQueryService queryService,
        TaskCommentRepository commentRepository,
        RoleRepository roleRepository,
        WorkflowInstanceRepository instanceRepository,
        WorkflowDefinitionVersionRepository versionRepository,
        WorkflowRuntime runtime,
        WorkflowAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.taskRepository = taskRepository;
        this.assignmentService = assignmentService;
        this.queryService = queryService;
        this.commentRepository = commentRepository;
        this.roleRepository = roleRepository;
        this.instanceRepository = instanceRepository;
        this.versionRepository = versionRepository;
        this.runtime = runtime;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional
    public WorkflowTaskResponse assign(UUID id, TaskAssignRequest request, UUID actorId) {
        WorkflowTaskEntity task = requireOpen(id);
        if (request.userId() != null) {
            assignmentService.assignToUser(task, request.userId(), actorId, request.reason());
        } else if (request.roleCode() != null && !request.roleCode().isBlank()) {
            assignmentService.assignToRole(task, request.roleCode(), actorId, request.reason());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide userId or roleCode");
        }
        task.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.WORKFLOW_TASK_ASSIGNED, actorId, "workflow_task", id,
            auditHelper.mapOf("userId", request.userId(), "roleCode", request.roleCode()));
        return queryService.get(id);
    }

    @Transactional
    public WorkflowTaskResponse claim(UUID id, UUID actorId) {
        WorkflowTaskEntity task = require(id);
        WorkflowTaskStatus status = WorkflowTaskStatus.parse(task.getStatus());
        if (status != WorkflowTaskStatus.PENDING && status != WorkflowTaskStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task cannot be claimed in status " + status);
        }
        if (task.getAssigneeUserId() != null && !task.getAssigneeUserId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task assigned to another user");
        }
        if (task.getAssigneeUserId() == null) {
            if (task.getAssigneeRoleCode() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Task has no role pool to claim");
            }
            assertHasRole(actorId, task.getAssigneeRoleCode());
            UUID from = null;
            task.setAssigneeUserId(actorId);
            task.setStatus(WorkflowTaskStatus.IN_PROGRESS.name());
            assignmentService.recordClaim(task, from, actorId);
        } else {
            task.setStatus(WorkflowTaskStatus.IN_PROGRESS.name());
        }
        task.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.WORKFLOW_TASK_CLAIMED, actorId, "workflow_task", id, auditHelper.mapOf());
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", id.toString());
        payload.put("title", task.getTitle());
        payload.put("assigneeUserId", actorId.toString());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.TASK_CLAIMED,
            actorId,
            task.getSubjectType(),
            task.getSubjectId(),
            null,
            payload
        ));
        return queryService.get(id);
    }

    @Transactional
    public WorkflowTaskResponse complete(UUID id, TaskCompleteRequest request, UUID actorId) {
        WorkflowTaskEntity task = requireOpen(id);
        assertCanAct(task, actorId);
        Instant now = Instant.now();
        task.setStatus(WorkflowTaskStatus.COMPLETED.name());
        task.setOutcome(request.outcome() == null || request.outcome().isBlank() ? "COMPLETED" : request.outcome());
        task.setCompletedAt(now);
        task.setCompletedBy(actorId);
        task.setUpdatedBy(actorId);

        auditHelper.record(AuditAction.WORKFLOW_TASK_COMPLETED, actorId, "workflow_task", id,
            auditHelper.mapOf("outcome", task.getOutcome(), "advanceAction", request.advanceAction()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", id.toString());
        payload.put("outcome", task.getOutcome());
        payload.put("title", task.getTitle());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.TASK_COMPLETED,
            actorId,
            task.getSubjectType(),
            task.getSubjectId(),
            null,
            payload
        ));

        if (request.advanceAction() != null && !request.advanceAction().isBlank()) {
            WorkflowInstanceEntity instance = instanceRepository.findByIdAndDeletedFalse(task.getInstanceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Workflow instance missing"));
            WorkflowDefinitionVersionEntity version = versionRepository.findByIdAndDeletedFalse(instance.getDefinitionVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Pinned version missing"));
            runtime.transition(instance, version, request.advanceAction(), request.reason(), actorId);
        }
        return queryService.get(id);
    }

    @Transactional
    public WorkflowTaskResponse cancel(UUID id, TaskCancelRequest request, UUID actorId) {
        WorkflowTaskEntity task = requireOpen(id);
        task.setStatus(WorkflowTaskStatus.CANCELLED.name());
        task.setOutcome(request.reason());
        task.setUpdatedBy(actorId);
        auditHelper.record(AuditAction.WORKFLOW_TASK_CANCELLED, actorId, "workflow_task", id,
            auditHelper.mapOf("reason", request.reason()));
        return queryService.get(id);
    }

    @Transactional
    public WorkflowTaskResponse addComment(UUID id, TaskCommentRequest request, UUID actorId) {
        require(id);
        TaskCommentEntity comment = new TaskCommentEntity();
        comment.setTaskId(id);
        comment.setAuthorId(actorId);
        comment.setBody(request.body().trim());
        comment.setVisibility(request.visibility() == null || request.visibility().isBlank()
            ? "INTERNAL" : request.visibility().trim().toUpperCase());
        comment.setStatus("ACTIVE");
        comment.setDeleted(false);
        comment.setCreatedBy(actorId);
        commentRepository.save(comment);
        return queryService.get(id);
    }

    private WorkflowTaskEntity require(UUID id) {
        return taskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    private WorkflowTaskEntity requireOpen(UUID id) {
        WorkflowTaskEntity task = require(id);
        if (WorkflowTaskStatus.parse(task.getStatus()).isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is already terminal: " + task.getStatus());
        }
        return task;
    }

    private void assertCanAct(WorkflowTaskEntity task, UUID actorId) {
        if (task.getAssigneeUserId() != null && task.getAssigneeUserId().equals(actorId)) {
            return;
        }
        if (task.getAssigneeRoleCode() != null && task.getAssigneeUserId() == null) {
            assertHasRole(actorId, task.getAssigneeRoleCode());
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assignee may complete this task");
    }

    private void assertHasRole(UUID userId, String roleCode) {
        Set<String> codes = roleRepository.findActiveRolesByUserId(userId).stream()
            .map(RoleEntity::getCode)
            .collect(Collectors.toSet());
        if (!codes.contains(roleCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User lacks role " + roleCode);
        }
    }
}
