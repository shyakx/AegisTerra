package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record AgroecologicalZoneResponse(
    UUID id,
    String code,
    String name,
    String status
) {}
