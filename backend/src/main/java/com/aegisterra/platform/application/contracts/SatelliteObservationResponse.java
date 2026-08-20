package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SatelliteObservationResponse(
    UUID id,
    UUID farmId,
    String productId,
    String productVersion,
    Instant observedAt,
    BigDecimal cloudCoverPct,
    String storageUri,
    String providerCode,
    String qualityFlag
) {}
