package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class TaxPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "TAXES";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal rate = ruleValue.path("rate").decimalValue();
        working.applyRateOfCurrent(factorCode(), ruleCode, rate, false, "Tax");
    }
}
