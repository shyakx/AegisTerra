package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record WeatherStationResponse(
    UUID id,
    String code,
    String name,
    BigDecimal elevationM,
    String providerCode,
    String externalStationId,
    String districtCode,
    Double longitude,
    Double latitude,
    String status
) {}
