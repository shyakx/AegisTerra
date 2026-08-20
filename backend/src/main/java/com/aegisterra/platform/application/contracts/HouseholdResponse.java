package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record HouseholdResponse(
    UUID id,
    String code,
    String headName,
    String status
) {}
