package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record ClimateRasterLayerResponse(
    UUID id,
    String code,
    String name,
    String providerCode,
    String variableCode,
    String storageUri,
    String format,
    String status
) {}
