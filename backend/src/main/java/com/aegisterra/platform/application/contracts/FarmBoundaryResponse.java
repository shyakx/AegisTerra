package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FarmBoundaryResponse(
    UUID id,
    UUID farmId,
    String geoJson,
    String source,
    Instant capturedAt,
    BigDecimal areaHa,
    String status
) {}
