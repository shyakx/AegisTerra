package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.workflow.WorkflowGraph;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowInstanceEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowStepEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowTaskFactory {

    private final WorkflowTaskRepository taskRepository;
    private final TaskAssignmentService assignmentService;
    private final WorkflowAuditHelper auditHelper;
    private final EventBus eventBus;

    public WorkflowTaskFactory(
        WorkflowTaskRepository taskRepository,
        TaskAssignmentService assignmentService,
        WorkflowAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.taskRepository = taskRepository;
        this.assignmentService = assignmentService;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional
    public List<WorkflowTaskEntity> createForStep(
        WorkflowInstanceEntity instance,
        WorkflowStepEntity step,
        WorkflowGraph graph,
        UUID actorId
    ) {
        return graph.taskConfig(step.getStepCode()).map(config -> {
            WorkflowTaskEntity task = new WorkflowTaskEntity();
            task.setInstanceId(instance.getId());
            task.setWorkflowStepId(step.getId());
            task.setStepCode(step.getStepCode());
            task.setTaskType(config.taskType().name());
            task.setTitle(config.title());
            task.setDescription(config.description());
            task.setSubjectType(instance.getSubjectType());
            task.setSubjectId(instance.getSubjectId());
            task.setPriority(0);
            task.setStatus(WorkflowTaskStatus.PENDING.name());
            task.setDeleted(false);
            task.setCreatedBy(actorId);
            taskRepository.save(task);

            if (config.assignees().isEmpty()) {
                // Unassigned pending — ops must assign
            } else {
                WorkflowGraph.AssigneeRule first = config.assignees().getFirst();
                assignmentService.applyInitial(task, first.strategy(), first.value(), actorId);
            }
            taskRepository.save(task);

            auditHelper.record(AuditAction.WORKFLOW_TASK_CREATED, actorId, "workflow_task", task.getId(),
                auditHelper.mapOf("instanceId", instance.getId(), "stepCode", step.getStepCode(), "taskType", task.getTaskType()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("taskId", task.getId().toString());
            payload.put("instanceId", instance.getId().toString());
            payload.put("stepCode", task.getStepCode());
            payload.put("title", task.getTitle());
            if (task.getAssigneeUserId() != null) {
                payload.put("assigneeUserId", task.getAssigneeUserId().toString());
            }
            if (task.getAssigneeRoleCode() != null) {
                payload.put("assigneeRoleCode", task.getAssigneeRoleCode());
            }
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.TASK_CREATED,
                actorId,
                task.getSubjectType(),
                task.getSubjectId(),
                instance.getCorrelationId(),
                payload
            ));
            return List.of(task);
        }).orElse(List.of());
    }

    @Transactional
    public void cancelOpenTasksForStep(UUID instanceId, String stepCode, UUID actorId) {
        taskRepository.findByInstanceIdAndStepCodeAndDeletedFalse(instanceId, stepCode).forEach(task -> {
            WorkflowTaskStatus status = WorkflowTaskStatus.parse(task.getStatus());
            if (status.isOpen()) {
                task.setStatus(WorkflowTaskStatus.CANCELLED.name());
                task.setUpdatedBy(actorId);
                auditHelper.record(AuditAction.WORKFLOW_TASK_CANCELLED, actorId, "workflow_task", task.getId(),
                    auditHelper.mapOf("reason", "step-left", "stepCode", stepCode));
            }
        });
    }
}
