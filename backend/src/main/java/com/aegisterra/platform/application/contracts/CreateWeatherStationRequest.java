package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWeatherStationRequest(
    String code,
    String name,
    double longitude,
    double latitude,
    BigDecimal elevationM,
    String providerCode,
    String externalStationId,
    String districtCode
) {}
