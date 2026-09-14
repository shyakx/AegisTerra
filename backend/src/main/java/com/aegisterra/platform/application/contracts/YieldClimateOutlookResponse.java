package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;

/**
 * Explainable crop yield outlook for stakeholder planning (ADR-010).
 * Past decade → recent seasons → predicted yield under current climate risk.
 */
public record YieldClimateOutlookResponse(
    int referenceYear,
    int pastWindowStartYear,
    int pastWindowEndYear,
    int recentWindowStartYear,
    int recentWindowEndYear,
    Double nationalMeanRiskScore,
    String nationalRiskGrade,
    List<CropOutlook> crops,
    String methodology,
    Instant generatedAt
) {
    public record YearYield(int year, Double meanYieldTHa, int sampleCount) {}

    public record CropOutlook(
        String cropCode,
        String cropName,
        Double pastMeanYieldTHa,
        Double recentMeanYieldTHa,
        Double predictedYieldTHa,
        Double yieldChangePctRecentVsPast,
        String outlookLabel,
        String narrative,
        int farmCount,
        int seasonSampleCount,
        List<YearYield> yearlySeries
    ) {}
}
