package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "coverage_limits")
public class CoverageLimitEntity extends AuditableEntity {

    @Column(name = "coverage_package_id", nullable = false)
    private UUID coveragePackageId;

    @Column(name = "peril_code", nullable = false, length = 64)
    private String perilCode;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    public UUID getCoveragePackageId() { return coveragePackageId; }
    public void setCoveragePackageId(UUID coveragePackageId) { this.coveragePackageId = coveragePackageId; }
    public String getPerilCode() { return perilCode; }
    public void setPerilCode(String perilCode) { this.perilCode = perilCode; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
