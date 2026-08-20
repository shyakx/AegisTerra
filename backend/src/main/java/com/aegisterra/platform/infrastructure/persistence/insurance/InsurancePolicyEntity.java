package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "insurance_policies")
public class InsurancePolicyEntity extends AuditableEntity {

    @Column(name = "policy_number", nullable = false, length = 64)
    private String policyNumber;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "farm_id", nullable = false)
    private UUID farmId;

    @Column(name = "policy_type_id", nullable = false)
    private UUID policyTypeId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "coverage_package_id")
    private UUID coveragePackageId;

    @Column(name = "crop_season_id")
    private UUID cropSeasonId;

    @Column(name = "crop_id")
    private UUID cropId;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "premium_quote_id")
    private UUID premiumQuoteId;

    @Column(name = "parent_policy_id")
    private UUID parentPolicyId;

    @Column(name = "insurance_company_id")
    private UUID insuranceCompanyId;

    @Column(name = "coverage_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal coverageAmount;

    @Column(name = "premium_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal premiumAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "transition_reason", length = 1000)
    private String transitionReason;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "coverage_snapshot_json", columnDefinition = "text")
    private String coverageSnapshotJson;

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }
    public UUID getFarmId() { return farmId; }
    public void setFarmId(UUID farmId) { this.farmId = farmId; }
    public UUID getPolicyTypeId() { return policyTypeId; }
    public void setPolicyTypeId(UUID policyTypeId) { this.policyTypeId = policyTypeId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getCoveragePackageId() { return coveragePackageId; }
    public void setCoveragePackageId(UUID coveragePackageId) { this.coveragePackageId = coveragePackageId; }
    public UUID getCropSeasonId() { return cropSeasonId; }
    public void setCropSeasonId(UUID cropSeasonId) { this.cropSeasonId = cropSeasonId; }
    public UUID getCropId() { return cropId; }
    public void setCropId(UUID cropId) { this.cropId = cropId; }
    public UUID getSeasonId() { return seasonId; }
    public void setSeasonId(UUID seasonId) { this.seasonId = seasonId; }
    public UUID getPremiumQuoteId() { return premiumQuoteId; }
    public void setPremiumQuoteId(UUID premiumQuoteId) { this.premiumQuoteId = premiumQuoteId; }
    public UUID getParentPolicyId() { return parentPolicyId; }
    public void setParentPolicyId(UUID parentPolicyId) { this.parentPolicyId = parentPolicyId; }
    public UUID getInsuranceCompanyId() { return insuranceCompanyId; }
    public void setInsuranceCompanyId(UUID insuranceCompanyId) { this.insuranceCompanyId = insuranceCompanyId; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(BigDecimal coverageAmount) { this.coverageAmount = coverageAmount; }
    public BigDecimal getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(BigDecimal premiumAmount) { this.premiumAmount = premiumAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public String getTransitionReason() { return transitionReason; }
    public void setTransitionReason(String transitionReason) { this.transitionReason = transitionReason; }
    public String getCoverageSnapshotJson() { return coverageSnapshotJson; }
    public void setCoverageSnapshotJson(String coverageSnapshotJson) { this.coverageSnapshotJson = coverageSnapshotJson; }
}
