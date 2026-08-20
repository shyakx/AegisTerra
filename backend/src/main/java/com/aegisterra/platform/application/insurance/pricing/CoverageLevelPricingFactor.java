package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class CoverageLevelPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "COVERAGE_LEVEL";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        if (context.coverageLevelPct() == null) {
            return;
        }
        BigDecimal factor = context.coverageLevelPct().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        working.applyMultiplier(factorCode(), ruleCode, factor, "Coverage level scaling");
    }
}
