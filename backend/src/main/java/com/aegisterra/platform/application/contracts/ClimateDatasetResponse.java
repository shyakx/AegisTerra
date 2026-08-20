package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateDatasetResponse(
    UUID id,
    String code,
    String name,
    String description,
    String datasetType,
    String providerCode,
    String status,
    Instant timeStart,
    Instant timeEnd
) {}
