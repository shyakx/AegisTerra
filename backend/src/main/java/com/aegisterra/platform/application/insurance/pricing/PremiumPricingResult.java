package com.aegisterra.platform.application.insurance.pricing;

import java.math.BigDecimal;
import java.util.List;

public record PremiumPricingResult(
    BigDecimal baseAmount,
    BigDecimal grossAmount,
    BigDecimal netAmount,
    BigDecimal coverageAmount,
    String currency,
    List<PremiumPricingContext.FactorAdjustment> adjustments
) {}
