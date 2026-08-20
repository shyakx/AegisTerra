package com.aegisterra.platform.application.workflow.assignment;

import com.aegisterra.platform.domain.workflow.AssignmentStrategyCode;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowTaskEntity;

/** SPI for assigning tasks. Stage 6B implements USER and ROLE. */
public interface AssignmentStrategy {
    AssignmentStrategyCode code();

    void apply(WorkflowTaskEntity task, String value);
}
