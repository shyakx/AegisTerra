package com.aegisterra.platform.domain.climateintelligence;

public enum RiskGrade {
    LOW,
    MODERATE,
    HIGH,
    EXTREME,
    INSUFFICIENT_DATA;

    public static RiskGrade fromScore(double score) {
        if (score < 25) {
            return LOW;
        }
        if (score < 50) {
            return MODERATE;
        }
        if (score < 75) {
            return HIGH;
        }
        return EXTREME;
    }
}
