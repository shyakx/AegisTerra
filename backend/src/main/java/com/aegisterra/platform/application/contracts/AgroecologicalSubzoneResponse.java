package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record AgroecologicalSubzoneResponse(
    UUID id,
    String code,
    String name,
    String status,
    UUID zoneId,
    String zoneCode,
    String zoneName
) {}
