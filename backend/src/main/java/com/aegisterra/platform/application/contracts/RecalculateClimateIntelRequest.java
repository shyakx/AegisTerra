package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record RecalculateClimateIntelRequest(
    UUID farmId,
    String districtCode,
    Instant from,
    Instant to
) {}
