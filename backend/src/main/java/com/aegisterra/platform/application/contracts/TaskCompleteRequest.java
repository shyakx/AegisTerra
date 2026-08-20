package com.aegisterra.platform.application.contracts;

public record TaskCompleteRequest(
    String outcome,
    String reason,
    String advanceAction
) {}
