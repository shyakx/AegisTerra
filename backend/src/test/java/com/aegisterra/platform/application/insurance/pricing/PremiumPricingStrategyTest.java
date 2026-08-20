package com.aegisterra.platform.application.insurance.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumPricingRuleEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PremiumPricingStrategyTest {

    @Test
    void areaMultiplierAppliesConfiguredFactorsInPriorityOrder() {
        ObjectMapper mapper = new ObjectMapper();
        PremiumPricingContext ctx = new PremiumPricingContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            "MAIZE",
            "MEDIUM",
            new BigDecimal("2.0"),
            new BigDecimal("80"),
            new BigDecimal("600000"),
            "RWF",
            new BigDecimal("1.0")
        );

        List<PremiumPricingRuleEntity> rules = List.of(
            rule("CMP_BASE", "INSURANCE_PRODUCT", 10, "{\"baseRatePerHa\":25000}"),
            rule("CMP_CROP", "CROP_TYPE", 20, "{\"defaultMultiplier\":1.0,\"byCropCode\":{\"MAIZE\":1.1}}"),
            rule("CMP_AREA", "FARM_AREA", 30, "{\"mode\":\"PER_HA\"}"),
            rule("CMP_RISK", "RISK_ZONE", 40, "{\"defaultMultiplier\":1.0,\"byZone\":{\"MEDIUM\":1.0}}"),
            rule("CMP_WEATHER", "HISTORICAL_WEATHER", 50, "{\"defaultMultiplier\":1.0}"),
            rule("CMP_COV", "COVERAGE_LEVEL", 60, "{\"mode\":\"PCT_OF_PACKAGE\"}"),
            rule("CMP_SUB", "GOVERNMENT_SUBSIDY", 70, "{\"rate\":0.40}"),
            rule("CMP_DISC", "DISCOUNT_RULES", 80, "{\"maxRate\":0.10,\"defaultRate\":0.0}"),
            rule("CMP_PARTNER", "PARTNER_AGREEMENTS", 90, "{\"defaultMultiplier\":1.0}"),
            rule("CMP_TAX", "TAXES", 100, "{\"rate\":0.18}")
        );

        List<PricingFactor> factors = List.of(
            new CropTypePricingFactor(),
            new FarmAreaPricingFactor(),
            new RiskZonePricingFactor(),
            new HistoricalWeatherPricingFactor(),
            new CoverageLevelPricingFactor(),
            new SubsidyPricingFactor(),
            new DiscountPricingFactor(),
            new PartnerAgreementPricingFactor(),
            new TaxPricingFactor()
        );

        PremiumPricingResult result = new AreaMultiplierPricingStrategy(mapper).price(ctx, rules, factors);

        // 25000*2ha=50000; *1.1 crop=55000; *0.80 coverage=44000; *0.6 subsidy=26400; *1.18 tax=31152
        assertEquals(0, new BigDecimal("50000.0000").compareTo(result.baseAmount()));
        assertEquals(0, new BigDecimal("31152.0000").compareTo(result.netAmount()));
        assertEquals(0, new BigDecimal("960000.0000").compareTo(result.coverageAmount()));
        assertEquals("RWF", result.currency());
    }

    private static PremiumPricingRuleEntity rule(String code, String factor, int priority, String json) {
        PremiumPricingRuleEntity entity = new PremiumPricingRuleEntity();
        entity.setRuleCode(code);
        entity.setFactorCode(factor);
        entity.setPriority(priority);
        entity.setValueJson(json);
        return entity;
    }
}
