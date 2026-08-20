package com.aegisterra.platform.domain.workflow;

public enum WorkflowTaskType {
    GENERIC,
    APPROVAL,
    VERIFICATION,
    INSPECTION,
    FINANCE;

    public static WorkflowTaskType parse(String value) {
        if (value == null || value.isBlank()) {
            return GENERIC;
        }
        return WorkflowTaskType.valueOf(value.trim().toUpperCase());
    }
}
