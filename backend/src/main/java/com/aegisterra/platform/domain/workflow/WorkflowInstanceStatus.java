package com.aegisterra.platform.domain.workflow;

public enum WorkflowInstanceStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    REJECTED,
    CANCELLED;

    public static WorkflowInstanceStatus parse(String value) {
        return WorkflowInstanceStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED;
    }
}
