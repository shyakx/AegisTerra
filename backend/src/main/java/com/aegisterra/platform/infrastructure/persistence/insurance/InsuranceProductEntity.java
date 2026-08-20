package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "insurance_products")
public class InsuranceProductEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "pricing_strategy_code", nullable = false, length = 64)
    private String pricingStrategyCode;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "eligibility_json", columnDefinition = "text")
    private String eligibilityJson;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPricingStrategyCode() { return pricingStrategyCode; }
    public void setPricingStrategyCode(String pricingStrategyCode) { this.pricingStrategyCode = pricingStrategyCode; }
    public String getEligibilityJson() { return eligibilityJson; }
    public void setEligibilityJson(String eligibilityJson) { this.eligibilityJson = eligibilityJson; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
