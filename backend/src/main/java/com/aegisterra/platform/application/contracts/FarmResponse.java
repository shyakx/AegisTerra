package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record FarmResponse(
    UUID id,
    UUID farmerId,
    String farmCode,
    String farmName,
    BigDecimal farmSizeHa,
    String cropType,
    UUID districtId,
    UUID sectorId,
    UUID cellId,
    UUID villageId,
    String status
) {}
