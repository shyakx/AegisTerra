package com.aegisterra.platform.application.workflow.assignment;

import com.aegisterra.platform.domain.workflow.AssignmentStrategyCode;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.RoleRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RoleAssignmentStrategy implements AssignmentStrategy {

    private final RoleRepository roleRepository;

    public RoleAssignmentStrategy(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public AssignmentStrategyCode code() {
        return AssignmentStrategyCode.ROLE;
    }

    @Override
    public void apply(WorkflowTaskEntity task, String value) {
        String roleCode = value.trim().toUpperCase();
        roleRepository.findByCodeAndDeletedFalse(roleCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee role not found: " + roleCode));
        task.setAssigneeUserId(null);
        task.setAssigneeRoleCode(roleCode);
        task.setStatus(WorkflowTaskStatus.PENDING.name());
    }
}
