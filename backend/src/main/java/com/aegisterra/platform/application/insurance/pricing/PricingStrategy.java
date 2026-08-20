package com.aegisterra.platform.application.insurance.pricing;

import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumPricingRuleEntity;
import java.util.List;

public interface PricingStrategy {
    String code();

    PremiumPricingResult price(
        PremiumPricingContext context,
        List<PremiumPricingRuleEntity> rules,
        List<PricingFactor> factors
    );
}
