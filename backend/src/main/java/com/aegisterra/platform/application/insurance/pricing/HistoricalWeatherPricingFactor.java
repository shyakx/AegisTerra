package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class HistoricalWeatherPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "HISTORICAL_WEATHER";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal multiplier = context.weatherMultiplier() != null
            ? context.weatherMultiplier()
            : ruleValue.path("defaultMultiplier").decimalValue();
        working.applyMultiplier(factorCode(), ruleCode, multiplier, "Historical weather loading");
    }
}
