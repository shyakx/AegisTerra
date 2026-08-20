package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claims")
public class ClaimEntity extends AuditableEntity {

    @Column(name = "claim_number", nullable = false, length = 64)
    private String claimNumber;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "claimed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal claimedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(length = 2000)
    private String description;

    @Column(name = "claim_type_code", length = 64)
    private String claimTypeCode;

    @Column(name = "farmer_id")
    private UUID farmerId;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "season_id")
    private UUID seasonId;

    @Column(name = "crop_id")
    private UUID cropId;

    @Column(name = "cause_of_loss", length = 255)
    private String causeOfLoss;

    @Column(name = "assessed_amount", precision = 19, scale = 4)
    private BigDecimal assessedAmount;

    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    @Column(name = "coverage_snapshot_json", columnDefinition = "text")
    private String coverageSnapshotJson;

    @Column(name = "financial_snapshot_json", columnDefinition = "text")
    private String financialSnapshotJson;

    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    @Column(name = "workflow_definition_code", length = 64)
    private String workflowDefinitionCode;

    @Column(name = "fraud_tier", length = 16)
    private String fraudTier;

    @Column(name = "fraud_score", precision = 10, scale = 4)
    private BigDecimal fraudScore;

    @Column(name = "fraud_flags_json", columnDefinition = "text")
    private String fraudFlagsJson;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }
    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public void setClaimedAmount(BigDecimal claimedAmount) { this.claimedAmount = claimedAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClaimTypeCode() { return claimTypeCode; }
    public void setClaimTypeCode(String claimTypeCode) { this.claimTypeCode = claimTypeCode; }
    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }
    public UUID getFarmId() { return farmId; }
    public void setFarmId(UUID farmId) { this.farmId = farmId; }
    public UUID getSeasonId() { return seasonId; }
    public void setSeasonId(UUID seasonId) { this.seasonId = seasonId; }
    public UUID getCropId() { return cropId; }
    public void setCropId(UUID cropId) { this.cropId = cropId; }
    public String getCauseOfLoss() { return causeOfLoss; }
    public void setCauseOfLoss(String causeOfLoss) { this.causeOfLoss = causeOfLoss; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public void setAssessedAmount(BigDecimal assessedAmount) { this.assessedAmount = assessedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
    public String getCoverageSnapshotJson() { return coverageSnapshotJson; }
    public void setCoverageSnapshotJson(String coverageSnapshotJson) { this.coverageSnapshotJson = coverageSnapshotJson; }
    public String getFinancialSnapshotJson() { return financialSnapshotJson; }
    public void setFinancialSnapshotJson(String financialSnapshotJson) { this.financialSnapshotJson = financialSnapshotJson; }
    public UUID getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(UUID workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    public String getWorkflowDefinitionCode() { return workflowDefinitionCode; }
    public void setWorkflowDefinitionCode(String workflowDefinitionCode) { this.workflowDefinitionCode = workflowDefinitionCode; }
    public String getFraudTier() { return fraudTier; }
    public void setFraudTier(String fraudTier) { this.fraudTier = fraudTier; }
    public BigDecimal getFraudScore() { return fraudScore; }
    public void setFraudScore(BigDecimal fraudScore) { this.fraudScore = fraudScore; }
    public String getFraudFlagsJson() { return fraudFlagsJson; }
    public void setFraudFlagsJson(String fraudFlagsJson) { this.fraudFlagsJson = fraudFlagsJson; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
