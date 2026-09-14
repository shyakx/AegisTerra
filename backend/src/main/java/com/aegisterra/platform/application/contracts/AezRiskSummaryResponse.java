package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;

/**
 * Read-time AEZ climate risk rollups from latest district_risk_snapshots joined to the geography catalog.
 */
public record AezRiskSummaryResponse(
    List<ZoneHeat> zones,
    List<SubzoneHeat> subzones,
    List<String> unmappedDistrictCodes,
    int unmappedCount,
    Instant generatedAt
) {
    public record ZoneHeat(
        String code,
        String name,
        Double meanScore,
        String grade,
        int districtCount,
        int farmCount
    ) {}

    public record SubzoneHeat(
        String code,
        String name,
        String zoneCode,
        String zoneName,
        Double meanScore,
        String grade,
        int districtCount,
        int farmCount
    ) {}
}
