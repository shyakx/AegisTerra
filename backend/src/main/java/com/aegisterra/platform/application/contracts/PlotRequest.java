package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PlotRequest(
    @NotNull UUID farmId,
    @NotBlank @Size(max = 64) String plotCode,
    @Size(max = 150) String name,
    String geoJson,
    BigDecimal areaHa,
    String status,
    String reason
) {}
