package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record PlotResponse(
    UUID id,
    UUID farmId,
    String plotCode,
    String name,
    String geoJson,
    BigDecimal areaHa,
    String status
) {}
