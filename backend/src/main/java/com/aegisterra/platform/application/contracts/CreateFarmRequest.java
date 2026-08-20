package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record CreateFarmRequest(
    UUID farmerId,
    String farmName,
    String farmCode,
    String cropType,
    Double farmSizeHa,
    UUID districtId
) {
}
