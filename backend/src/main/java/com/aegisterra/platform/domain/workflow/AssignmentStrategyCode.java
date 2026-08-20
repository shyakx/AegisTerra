package com.aegisterra.platform.domain.workflow;

/** Assignment target strategy codes. USER and ROLE are Stage 6B; others are extension points. */
public enum AssignmentStrategyCode {
    USER,
    ROLE,
    DEPARTMENT,
    REGION;

    public static AssignmentStrategyCode parse(String value) {
        return AssignmentStrategyCode.valueOf(value.trim().toUpperCase());
    }
}
