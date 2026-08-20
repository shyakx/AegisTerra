package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CropTypePricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "CROP_TYPE";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal multiplier = ruleValue.path("defaultMultiplier").decimalValue();
        if (context.cropCode() != null && ruleValue.path("byCropCode").has(context.cropCode())) {
            multiplier = ruleValue.path("byCropCode").path(context.cropCode()).decimalValue();
        }
        working.applyMultiplier(factorCode(), ruleCode, multiplier, "Crop multiplier");
    }
}
