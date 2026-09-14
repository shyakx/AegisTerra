package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record NationalRiskDashboardResponse(
    Map<String, Integer> farmsByGrade,
    long openCriticalAlerts,
    long openAlerts,
    List<DistrictHeat> districtHeat,
    Double dataCoveragePct,
    Instant generatedAt,
    List<AezRiskSummaryResponse.ZoneHeat> zoneHeat,
    List<AezRiskSummaryResponse.SubzoneHeat> subzoneHeat,
    List<String> unmappedDistrictCodes,
    int unmappedCount
) {
    public record DistrictHeat(String districtCode, Double meanScore, String grade) {}
}
