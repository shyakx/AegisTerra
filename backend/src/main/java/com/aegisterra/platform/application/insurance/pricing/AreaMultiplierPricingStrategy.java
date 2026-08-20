package com.aegisterra.platform.application.insurance.pricing;

import com.aegisterra.platform.infrastructure.persistence.insurance.PremiumPricingRuleEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AreaMultiplierPricingStrategy implements PricingStrategy {

    private final ObjectMapper objectMapper;

    public AreaMultiplierPricingStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String code() {
        return "AREA_MULTIPLIER";
    }

    @Override
    public PremiumPricingResult price(
        PremiumPricingContext context,
        List<PremiumPricingRuleEntity> rules,
        List<PricingFactor> factors
    ) {
        Map<String, PricingFactor> factorMap = factors.stream()
            .collect(Collectors.toMap(PricingFactor::factorCode, Function.identity(), (a, b) -> a));

        BigDecimal baseRate = BigDecimal.ZERO;
        PremiumPricingRuleEntity baseRule = rules.stream()
            .filter(r -> "INSURANCE_PRODUCT".equals(r.getFactorCode()))
            .findFirst()
            .orElse(null);
        if (baseRule != null) {
            JsonNode node = read(baseRule.getValueJson());
            baseRate = node.path("baseRatePerHa").decimalValue();
        }

        BigDecimal area = context.areaHa() == null || context.areaHa().signum() <= 0
            ? BigDecimal.ONE
            : context.areaHa();
        BigDecimal baseAmount = baseRate.multiply(area).setScale(4, RoundingMode.HALF_UP);
        PremiumPricingContext.WorkingAmount working = new PremiumPricingContext.WorkingAmount(baseAmount);

        List<PremiumPricingRuleEntity> ordered = rules.stream()
            .sorted(Comparator.comparingInt(PremiumPricingRuleEntity::getPriority))
            .toList();

        for (PremiumPricingRuleEntity rule : ordered) {
            if ("INSURANCE_PRODUCT".equals(rule.getFactorCode()) || "FARM_AREA".equals(rule.getFactorCode())) {
                continue; // already applied in base
            }
            PricingFactor factor = factorMap.get(rule.getFactorCode());
            if (factor != null) {
                factor.apply(context, working, read(rule.getValueJson()), rule.getRuleCode());
            }
        }

        BigDecimal coverageAmount = context.sumInsuredPerHa()
            .multiply(area)
            .multiply(context.coverageLevelPct() == null
                ? BigDecimal.ONE
                : context.coverageLevelPct().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP))
            .setScale(4, RoundingMode.HALF_UP);

        BigDecimal net = working.getAmount().max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
        return new PremiumPricingResult(
            baseAmount,
            net,
            net,
            coverageAmount,
            context.currency(),
            List.copyOf(working.getAdjustments())
        );
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }
}
