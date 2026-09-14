package com.aegisterra.platform.application.contracts;

import java.time.Instant;

public record DistrictRiskProfileResponse(
    String districtCode,
    Double meanScore,
    Double p90Score,
    int farmCount,
    int openAlertCount,
    String grade,
    String metricsJson,
    Instant generatedAt,
    String provinceCode,
    String provinceName,
    String agroecologicalZoneCode,
    String agroecologicalZoneName,
    String agroecologicalSubzoneCode,
    String agroecologicalSubzoneName
) {}
