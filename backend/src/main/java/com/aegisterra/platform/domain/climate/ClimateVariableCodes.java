package com.aegisterra.platform.domain.climate;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

public final class ClimateVariableCodes {
    public static final String TEMP_C = "TEMP_C";
    public static final String RAIN_MM = "RAIN_MM";
    public static final String HUMIDITY_PCT = "HUMIDITY_PCT";
    public static final String WIND_MS = "WIND_MS";
    public static final String PRESSURE_HPA = "PRESSURE_HPA";

    private ClimateVariableCodes() {}

    public static String canonical(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static Optional<String> defaultUnit(String variableCode) {
        return switch (canonical(variableCode)) {
            case TEMP_C -> Optional.of("C");
            case RAIN_MM -> Optional.of("mm");
            case HUMIDITY_PCT -> Optional.of("%");
            case WIND_MS -> Optional.of("m/s");
            case PRESSURE_HPA -> Optional.of("hPa");
            case null, default -> Optional.empty();
        };
    }

    public static boolean inPhysicalRange(String variableCode, BigDecimal value) {
        if (value == null) {
            return false;
        }
        double v = value.doubleValue();
        return switch (canonical(variableCode)) {
            case TEMP_C -> v >= -90 && v <= 60;
            case RAIN_MM -> v >= 0 && v <= 1000;
            case HUMIDITY_PCT -> v >= 0 && v <= 100;
            case WIND_MS -> v >= 0 && v <= 150;
            case PRESSURE_HPA -> v >= 800 && v <= 1100;
            case null, default -> true;
        };
    }
}
