package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DiscountPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "DISCOUNT_RULES";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal rate = ruleValue.path("defaultRate").decimalValue();
        BigDecimal max = ruleValue.path("maxRate").decimalValue();
        if (rate.compareTo(max) > 0) {
            rate = max;
        }
        if (rate.signum() > 0) {
            working.applyRateOfCurrent(factorCode(), ruleCode, rate, true, "Discount");
        }
    }
}
