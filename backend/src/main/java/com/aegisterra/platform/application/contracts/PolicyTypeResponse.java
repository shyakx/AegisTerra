package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record PolicyTypeResponse(
    UUID id,
    String code,
    String name,
    String description,
    String coverageRulesJson,
    UUID productId,
    String status
) {}
