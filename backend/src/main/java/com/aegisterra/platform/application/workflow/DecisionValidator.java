package com.aegisterra.platform.application.workflow;

import com.aegisterra.platform.domain.workflow.DecisionEffect;
import com.aegisterra.platform.domain.workflow.WorkflowGraph;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.DecisionTypeEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import com.aegisterra.platform.application.contracts.TaskDecisionRequest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DecisionValidator {

    private final RoleRepository roleRepository;

    public DecisionValidator(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void validate(
        WorkflowTaskEntity task,
        DecisionTypeEntity type,
        TaskDecisionRequest request,
        DecisionRuleResolver.ResolvedRule rule,
        WorkflowGraph graph,
        UUID actorId
    ) {
        if (WorkflowTaskStatus.parse(task.getStatus()).isTerminal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is already terminal: " + task.getStatus());
        }
        if (!"ACTIVE".equalsIgnoreCase(type.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Decision type is not active");
        }
        assertCanAct(task, actorId);

        graph.taskConfig(task.getStepCode()).ifPresent(config -> {
            List<String> allowed = config.allowedDecisions();
            if (allowed != null && !allowed.isEmpty() && !allowed.contains(type.getCode())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Decision " + type.getCode() + " is not allowed on step " + task.getStepCode());
            }
        });

        if (type.isRequiresComment() && (request.comment() == null || request.comment().isBlank())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Comment required for decision " + type.getCode());
        }
        if (type.isRequiresTargetUser() && request.targetUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "targetUserId required for decision " + type.getCode());
        }
        if (rule.effect() == DecisionEffect.REASSIGN && request.targetUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "targetUserId required for REASSIGN effect");
        }

        boolean needsAdvance = rule.effect().advancesWorkflow()
            || (rule.effect() == DecisionEffect.CANCEL_TASK && rule.workflowAction() != null)
            || (rule.effect() == DecisionEffect.KEEP_OPEN && rule.workflowAction() != null);
        if (needsAdvance) {
            if (rule.workflowAction() == null || rule.workflowAction().isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Decision rule missing workflow_action for advancing effect");
            }
            graph.assertCanTransition(task.getStepCode(), rule.workflowAction());
        }
    }

    public void assertCanAct(WorkflowTaskEntity task, UUID actorId) {
        if (task.getAssigneeUserId() != null && task.getAssigneeUserId().equals(actorId)) {
            return;
        }
        if (task.getAssigneeRoleCode() != null && task.getAssigneeUserId() == null) {
            Set<String> codes = roleRepository.findActiveRolesByUserId(actorId).stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toSet());
            if (codes.contains(task.getAssigneeRoleCode())) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assignee may decide this task");
    }
}
