package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskAssignmentEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskAssignmentRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskCommentEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskCommentRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowEventRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskRepository;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.TaskAssignmentResponse;
import com.aegisterra.platform.application.contracts.TaskCommentResponse;
import com.aegisterra.platform.application.contracts.WorkflowEventResponse;
import com.aegisterra.platform.application.contracts.WorkflowTaskResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskQueryService {

    private static final List<String> OPEN = List.of(
        WorkflowTaskStatus.PENDING.name(),
        WorkflowTaskStatus.ASSIGNED.name(),
        WorkflowTaskStatus.IN_PROGRESS.name(),
        WorkflowTaskStatus.WAITING.name()
    );

    private final WorkflowTaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskCommentRepository commentRepository;
    private final WorkflowEventRepository eventRepository;
    private final RoleRepository roleRepository;

    public TaskQueryService(
        WorkflowTaskRepository taskRepository,
        TaskAssignmentRepository assignmentRepository,
        TaskCommentRepository commentRepository,
        WorkflowEventRepository eventRepository,
        RoleRepository roleRepository
    ) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkflowTaskResponse> search(
        String status,
        String subjectType,
        String taskType,
        UUID assigneeUserId,
        String roleCode,
        String q,
        Pageable pageable
    ) {
        Page<WorkflowTaskEntity> page = taskRepository.search(
            blankToNull(status),
            blankToNull(subjectType),
            blankToNull(taskType),
            assigneeUserId,
            blankToNull(roleCode),
            blankToNull(q),
            pageable
        );
        return PageResponse.from(page.map(t -> toSummary(t)));
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkflowTaskResponse> myInbox(
        UUID userId,
        String status,
        String subjectType,
        String q,
        Pageable pageable
    ) {
        List<String> roleCodes = roleRepository.findActiveRolesByUserId(userId).stream()
            .map(RoleEntity::getCode)
            .toList();
        if (roleCodes.isEmpty()) {
            roleCodes = List.of("__NONE__");
        }
        Page<WorkflowTaskEntity> page = taskRepository.searchMyInbox(
            userId,
            roleCodes,
            OPEN,
            blankToNull(status),
            blankToNull(subjectType),
            blankToNull(q),
            pageable
        );
        return PageResponse.from(page.map(this::toSummary));
    }

    @Transactional(readOnly = true)
    public WorkflowTaskResponse get(UUID id) {
        WorkflowTaskEntity task = taskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return toDetail(task);
    }

    private WorkflowTaskResponse toSummary(WorkflowTaskEntity t) {
        return new WorkflowTaskResponse(
            t.getId(), t.getInstanceId(), t.getWorkflowStepId(), t.getStepCode(), t.getTaskType(),
            t.getTitle(), t.getDescription(), t.getSubjectType(), t.getSubjectId(),
            t.getAssigneeUserId(), t.getAssigneeRoleCode(), t.getPriority(), t.getDueAt(),
            t.getStatus(), t.getOutcome(), t.getCompletedAt(), t.getCompletedBy(), t.getCreatedAt(),
            List.of(), List.of(), List.of()
        );
    }

    private WorkflowTaskResponse toDetail(WorkflowTaskEntity t) {
        List<TaskAssignmentResponse> assignments = assignmentRepository
            .findByTaskIdAndDeletedFalseOrderByOccurredAtAsc(t.getId()).stream()
            .map(this::toAssignment).toList();
        List<TaskCommentResponse> comments = commentRepository
            .findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(t.getId()).stream()
            .map(this::toComment).toList();
        List<WorkflowEventResponse> timeline = eventRepository
            .findByInstanceIdAndDeletedFalseOrderByOccurredAtAsc(t.getInstanceId()).stream()
            .map(e -> new WorkflowEventResponse(
                e.getId(), e.getEventType(), e.getFromStepCode(), e.getToStepCode(),
                e.getActionCode(), e.getActorId(), e.getMessage(), e.getOccurredAt()
            )).toList();
        return new WorkflowTaskResponse(
            t.getId(), t.getInstanceId(), t.getWorkflowStepId(), t.getStepCode(), t.getTaskType(),
            t.getTitle(), t.getDescription(), t.getSubjectType(), t.getSubjectId(),
            t.getAssigneeUserId(), t.getAssigneeRoleCode(), t.getPriority(), t.getDueAt(),
            t.getStatus(), t.getOutcome(), t.getCompletedAt(), t.getCompletedBy(), t.getCreatedAt(),
            assignments, comments, timeline
        );
    }

    private TaskAssignmentResponse toAssignment(TaskAssignmentEntity a) {
        return new TaskAssignmentResponse(
            a.getId(), a.getAction(), a.getFromUserId(), a.getToUserId(),
            a.getToRoleCode(), a.getActorId(), a.getReason(), a.getOccurredAt()
        );
    }

    private TaskCommentResponse toComment(TaskCommentEntity c) {
        return new TaskCommentResponse(c.getId(), c.getAuthorId(), c.getBody(), c.getVisibility(), c.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
