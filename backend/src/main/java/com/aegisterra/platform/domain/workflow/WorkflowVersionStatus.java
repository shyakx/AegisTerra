package com.aegisterra.platform.domain.workflow;

public enum WorkflowVersionStatus {
    DRAFT,
    PUBLISHED,
    RETIRED;

    public static WorkflowVersionStatus parse(String value) {
        return WorkflowVersionStatus.valueOf(value.trim().toUpperCase());
    }
}
