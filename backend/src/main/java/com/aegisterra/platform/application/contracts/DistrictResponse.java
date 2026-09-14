package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record DistrictResponse(
    UUID id,
    String code,
    String name,
    String status,
    UUID provinceId,
    String provinceCode,
    String provinceName,
    UUID agroecologicalSubzoneId,
    String agroecologicalSubzoneCode,
    String agroecologicalSubzoneName,
    UUID agroecologicalZoneId,
    String agroecologicalZoneCode,
    String agroecologicalZoneName
) {}
