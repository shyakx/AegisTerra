package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateImportJobResponse(
    UUID id,
    String jobNumber,
    String providerCode,
    String jobType,
    String status,
    int rowsRead,
    int rowsAccepted,
    int rowsRejected,
    Instant startedAt,
    Instant completedAt,
    String errorSummary,
    Instant createdAt
) {}
