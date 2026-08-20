package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Area is applied in AREA_MULTIPLIER base; this factor is registered for config completeness. */
@Component
public class FarmAreaPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "FARM_AREA";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        // no-op: area already multiplied into base by strategy
    }
}
