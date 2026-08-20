package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RiskZonePricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "RISK_ZONE";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal multiplier = ruleValue.path("defaultMultiplier").decimalValue();
        String zone = context.riskZoneCode() == null ? "MEDIUM" : context.riskZoneCode();
        if (ruleValue.path("byZone").has(zone)) {
            multiplier = ruleValue.path("byZone").path(zone).decimalValue();
        }
        working.applyMultiplier(factorCode(), ruleCode, multiplier, "Risk zone " + zone);
    }
}
