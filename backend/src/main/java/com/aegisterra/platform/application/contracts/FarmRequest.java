package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record FarmRequest(
    @NotNull UUID farmerId,
    @Size(max = 64) String farmCode,
    @NotBlank @Size(max = 255) String farmName,
    @DecimalMin("0.0") BigDecimal farmSizeHa,
    @Size(max = 100) String cropType,
    UUID districtId,
    UUID sectorId,
    UUID cellId,
    UUID villageId,
    String status,
    String reason
) {}
