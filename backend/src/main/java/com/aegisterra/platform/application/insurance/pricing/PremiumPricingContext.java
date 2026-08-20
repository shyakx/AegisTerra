package com.aegisterra.platform.application.insurance.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PremiumPricingContext {
    private final UUID productId;
    private final UUID coveragePackageId;
    private final UUID farmerId;
    private final UUID farmId;
    private final UUID cropId;
    private final UUID seasonId;
    private final UUID partnerId;
    private final String cropCode;
    private final String riskZoneCode;
    private final BigDecimal areaHa;
    private final BigDecimal coverageLevelPct;
    private final BigDecimal sumInsuredPerHa;
    private final String currency;
    private final BigDecimal weatherMultiplier;

    public PremiumPricingContext(
        UUID productId,
        UUID coveragePackageId,
        UUID farmerId,
        UUID farmId,
        UUID cropId,
        UUID seasonId,
        UUID partnerId,
        String cropCode,
        String riskZoneCode,
        BigDecimal areaHa,
        BigDecimal coverageLevelPct,
        BigDecimal sumInsuredPerHa,
        String currency,
        BigDecimal weatherMultiplier
    ) {
        this.productId = productId;
        this.coveragePackageId = coveragePackageId;
        this.farmerId = farmerId;
        this.farmId = farmId;
        this.cropId = cropId;
        this.seasonId = seasonId;
        this.partnerId = partnerId;
        this.cropCode = cropCode;
        this.riskZoneCode = riskZoneCode;
        this.areaHa = areaHa;
        this.coverageLevelPct = coverageLevelPct;
        this.sumInsuredPerHa = sumInsuredPerHa;
        this.currency = currency;
        this.weatherMultiplier = weatherMultiplier;
    }

    public UUID productId() { return productId; }
    public UUID coveragePackageId() { return coveragePackageId; }
    public UUID farmerId() { return farmerId; }
    public UUID farmId() { return farmId; }
    public UUID cropId() { return cropId; }
    public UUID seasonId() { return seasonId; }
    public UUID partnerId() { return partnerId; }
    public String cropCode() { return cropCode; }
    public String riskZoneCode() { return riskZoneCode; }
    public BigDecimal areaHa() { return areaHa; }
    public BigDecimal coverageLevelPct() { return coverageLevelPct; }
    public BigDecimal sumInsuredPerHa() { return sumInsuredPerHa; }
    public String currency() { return currency; }
    public BigDecimal weatherMultiplier() { return weatherMultiplier; }

    public record FactorAdjustment(String factorCode, String ruleCode, String kind, BigDecimal value, String note) {}

    public static final class WorkingAmount {
        private BigDecimal amount;
        private final List<FactorAdjustment> adjustments = new ArrayList<>();

        public WorkingAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public List<FactorAdjustment> getAdjustments() { return adjustments; }

        public void applyMultiplier(String factorCode, String ruleCode, BigDecimal multiplier, String note) {
            amount = amount.multiply(multiplier);
            adjustments.add(new FactorAdjustment(factorCode, ruleCode, "MULTIPLY", multiplier, note));
        }

        public void applyAdditive(String factorCode, String ruleCode, BigDecimal delta, String note) {
            amount = amount.add(delta);
            adjustments.add(new FactorAdjustment(factorCode, ruleCode, "ADD", delta, note));
        }

        public void applyRateOfCurrent(String factorCode, String ruleCode, BigDecimal rate, boolean subtract, String note) {
            BigDecimal delta = amount.multiply(rate);
            if (subtract) {
                amount = amount.subtract(delta);
                adjustments.add(new FactorAdjustment(factorCode, ruleCode, "SUBSIDY_RATE", rate, note));
            } else {
                amount = amount.add(delta);
                adjustments.add(new FactorAdjustment(factorCode, ruleCode, "TAX_RATE", rate, note));
            }
        }
    }
}
