package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.application.workflow.assignment.AssignmentStrategy;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.workflow.AssignmentStrategyCode;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskAssignmentEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.TaskAssignmentRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskAssignmentService {

    private final Map<AssignmentStrategyCode, AssignmentStrategy> strategies;
    private final TaskAssignmentRepository assignmentRepository;
    private final EventBus eventBus;

    public TaskAssignmentService(
        List<AssignmentStrategy> strategies,
        TaskAssignmentRepository assignmentRepository,
        EventBus eventBus
    ) {
        this.strategies = strategies.stream().collect(Collectors.toMap(AssignmentStrategy::code, Function.identity()));
        this.assignmentRepository = assignmentRepository;
        this.eventBus = eventBus;
    }

    @Transactional
    public void applyInitial(WorkflowTaskEntity task, AssignmentStrategyCode code, String value, UUID actorId) {
        AssignmentStrategy strategy = require(code);
        strategy.apply(task, value);
        record(task, "ASSIGN", null, task.getAssigneeUserId(), task.getAssigneeRoleCode(), actorId, "Initial assignment");
        publishAssigned(task, actorId);
    }

    @Transactional
    public void assignToUser(WorkflowTaskEntity task, UUID toUserId, UUID actorId, String reason) {
        UUID from = task.getAssigneeUserId();
        require(AssignmentStrategyCode.USER).apply(task, toUserId.toString());
        record(task, from == null ? "ASSIGN" : "REASSIGN", from, toUserId, null, actorId, reason);
        publishAssigned(task, actorId);
    }

    @Transactional
    public void assignToRole(WorkflowTaskEntity task, String roleCode, UUID actorId, String reason) {
        UUID from = task.getAssigneeUserId();
        require(AssignmentStrategyCode.ROLE).apply(task, roleCode);
        record(task, from == null ? "ASSIGN" : "REASSIGN", from, null, roleCode, actorId, reason);
        publishAssigned(task, actorId);
    }

    @Transactional
    public void recordClaim(WorkflowTaskEntity task, UUID fromUserId, UUID actorId) {
        record(task, "CLAIM", fromUserId, actorId, task.getAssigneeRoleCode(), actorId, "Claimed from role pool");
    }

    private void publishAssigned(WorkflowTaskEntity task, UUID actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", task.getId().toString());
        payload.put("title", task.getTitle());
        payload.put("stepCode", task.getStepCode());
        if (task.getAssigneeUserId() != null) {
            payload.put("assigneeUserId", task.getAssigneeUserId().toString());
        }
        if (task.getAssigneeRoleCode() != null) {
            payload.put("assigneeRoleCode", task.getAssigneeRoleCode());
        }
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.TASK_ASSIGNED,
            actorId,
            task.getSubjectType(),
            task.getSubjectId(),
            null,
            payload
        ));
    }

    private AssignmentStrategy require(AssignmentStrategyCode code) {
        AssignmentStrategy strategy = strategies.get(code);
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Assignment strategy not implemented in Stage 6B: " + code);
        }
        return strategy;
    }

    private void record(
        WorkflowTaskEntity task,
        String action,
        UUID fromUserId,
        UUID toUserId,
        String toRoleCode,
        UUID actorId,
        String reason
    ) {
        TaskAssignmentEntity row = new TaskAssignmentEntity();
        row.setTaskId(task.getId());
        row.setAction(action);
        row.setFromUserId(fromUserId);
        row.setToUserId(toUserId);
        row.setToRoleCode(toRoleCode);
        row.setActorId(actorId);
        row.setReason(reason);
        row.setOccurredAt(Instant.now());
        row.setStatus("ACTIVE");
        row.setDeleted(false);
        row.setCreatedBy(actorId);
        assignmentRepository.save(row);
    }
}
