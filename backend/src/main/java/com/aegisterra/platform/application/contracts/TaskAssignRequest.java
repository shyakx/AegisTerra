package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record TaskAssignRequest(
    UUID userId,
    String roleCode,
    String reason
) {}
