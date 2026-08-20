package com.aegisterra.platform.domain.workflow;

public enum WorkflowTaskStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    WAITING,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public static WorkflowTaskStatus parse(String value) {
        return WorkflowTaskStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean isOpen() {
        return this == PENDING || this == ASSIGNED || this == IN_PROGRESS || this == WAITING;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }
}
