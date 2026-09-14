package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;

/**
 * Experimental maize yield ML spike — feature extract + leave-one-year-out metrics.
 * Not a production forecast service.
 */
public record MaizeYieldMlSpikeResponse(
    String cropCode,
    int featureRowCount,
    List<FeatureRow> features,
    List<FoldResult> leaveOneYearOut,
    MetricsSummary metrics,
    String verdict,
    String notes,
    Instant generatedAt
) {
    public record FeatureRow(
        int harvestYear,
        double meanYieldTHa,
        int sampleCount,
        int farmCount,
        Double lag1YieldTHa,
        Double lag2YieldTHa,
        Double seasonRainMm,
        Double yearIndex
    ) {}

    public record FoldResult(
        int holdoutYear,
        double actualYieldTHa,
        Double naiveLastYearPred,
        Double ruleRecentMeanPred,
        Double linearLagRainPred,
        Double absErrorNaive,
        Double absErrorRule,
        Double absErrorLinear
    ) {}

    public record MetricsSummary(
        int folds,
        Double maeNaiveLastYear,
        Double maeRuleRecentMean,
        Double maeLinearLagRain,
        String bestMethod,
        boolean linearBeatsRule
    ) {}
}
