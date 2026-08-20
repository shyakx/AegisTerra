package com.aegisterra.platform.application.insurance.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PartnerAgreementPricingFactor implements PricingFactor {
    @Override
    public String factorCode() {
        return "PARTNER_AGREEMENTS";
    }

    @Override
    public void apply(PremiumPricingContext context, PremiumPricingContext.WorkingAmount working, JsonNode ruleValue, String ruleCode) {
        BigDecimal multiplier = ruleValue.path("defaultMultiplier").decimalValue();
        working.applyMultiplier(factorCode(), ruleCode, multiplier, "Partner agreement");
    }
}
