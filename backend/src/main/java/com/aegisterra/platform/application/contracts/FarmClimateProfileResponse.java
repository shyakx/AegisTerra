package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record FarmClimateProfileResponse(
    UUID id,
    UUID farmId,
    Instant generatedAt,
    String ruleVersion,
    String snapshotJson,
    Double latestRiskScore,
    String latestRiskGrade
) {}
