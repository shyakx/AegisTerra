package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateAlertResponse(
    UUID id,
    String alertNumber,
    String alertType,
    String severity,
    String scopeType,
    String scopeId,
    Instant validFrom,
    Instant validTo,
    String ruleVersion,
    String evidenceJson,
    String status,
    Instant createdAt
) {}
