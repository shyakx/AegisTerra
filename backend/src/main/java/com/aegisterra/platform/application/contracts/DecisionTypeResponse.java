package com.aegisterra.platform.application.contracts;

public record DecisionTypeResponse(
    String code,
    String name,
    String description,
    boolean requiresComment,
    boolean requiresTargetUser,
    int sortOrder,
    String status
) {}
