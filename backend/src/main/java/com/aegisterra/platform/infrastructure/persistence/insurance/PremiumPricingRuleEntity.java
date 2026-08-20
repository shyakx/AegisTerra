package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "premium_pricing_rules")
public class PremiumPricingRuleEntity extends AuditableEntity {

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "factor_code", nullable = false, length = 64)
    private String factorCode;

    @Column(nullable = false)
    private int priority;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "value_json", nullable = false, columnDefinition = "text")
    private String valueJson;

    @Column(length = 500)
    private String description;

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getFactorCode() { return factorCode; }
    public void setFactorCode(String factorCode) { this.factorCode = factorCode; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getValueJson() { return valueJson; }
    public void setValueJson(String valueJson) { this.valueJson = valueJson; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
