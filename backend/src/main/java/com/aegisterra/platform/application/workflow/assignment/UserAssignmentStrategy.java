package com.aegisterra.platform.application.workflow.assignment;

import com.aegisterra.platform.domain.workflow.AssignmentStrategyCode;
import com.aegisterra.platform.domain.workflow.WorkflowTaskStatus;
import com.aegisterra.platform.infrastructure.persistence.identity.UserRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserAssignmentStrategy implements AssignmentStrategy {

    private final UserRepository userRepository;

    public UserAssignmentStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AssignmentStrategyCode code() {
        return AssignmentStrategyCode.USER;
    }

    @Override
    public void apply(WorkflowTaskEntity task, String value) {
        UUID userId;
        try {
            userId = UUID.fromString(value.trim());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "USER assignment value must be a UUID");
        }
        userRepository.findByIdAndDeletedFalse(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee user not found"));
        task.setAssigneeUserId(userId);
        task.setAssigneeRoleCode(null);
        task.setStatus(WorkflowTaskStatus.ASSIGNED.name());
    }
}
