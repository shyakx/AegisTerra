package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateIntelJobResponse(
    UUID id,
    String jobNumber,
    String jobType,
    String status,
    int subjectsProcessed,
    String errorSummary,
    Instant startedAt,
    Instant completedAt,
    Instant createdAt
) {}
