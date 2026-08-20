package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "coverage_packages")
public class CoveragePackageEntity extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "policy_type_id")
    private UUID policyTypeId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "coverage_level_pct", precision = 7, scale = 4)
    private BigDecimal coverageLevelPct;

    @Column(name = "max_sum_insured", precision = 19, scale = 4)
    private BigDecimal maxSumInsured;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getPolicyTypeId() { return policyTypeId; }
    public void setPolicyTypeId(UUID policyTypeId) { this.policyTypeId = policyTypeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getCoverageLevelPct() { return coverageLevelPct; }
    public void setCoverageLevelPct(BigDecimal coverageLevelPct) { this.coverageLevelPct = coverageLevelPct; }
    public BigDecimal getMaxSumInsured() { return maxSumInsured; }
    public void setMaxSumInsured(BigDecimal maxSumInsured) { this.maxSumInsured = maxSumInsured; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
