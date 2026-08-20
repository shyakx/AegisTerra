package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "claim_assessments")
public class ClaimAssessmentEntity extends AuditableEntity {

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Column(name = "assessor_user_id")
    private UUID assessorUserId;

    @Column(name = "assessed_amount", precision = 19, scale = 4)
    private BigDecimal assessedAmount;

    @Column(length = 2000)
    private String notes;

    @Column(name = "assessed_at")
    private Instant assessedAt;

    @Column(name = "inspection_id")
    private UUID inspectionId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "methods_json", nullable = false, columnDefinition = "text")
    private String methodsJson = "[]";

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "findings_json", columnDefinition = "text")
    private String findingsJson;

    @Column(name = "recommended_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal recommendedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "RWF";

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "fraud_hints_json", columnDefinition = "text")
    private String fraudHintsJson;

    @Column(name = "assessor_id")
    private UUID assessorId;

    @Column(nullable = false)
    private boolean accepted;

    public UUID getClaimId() { return claimId; }
    public void setClaimId(UUID claimId) { this.claimId = claimId; }
    public UUID getAssessorUserId() { return assessorUserId; }
    public void setAssessorUserId(UUID assessorUserId) { this.assessorUserId = assessorUserId; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public void setAssessedAmount(BigDecimal assessedAmount) { this.assessedAmount = assessedAmount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
    public UUID getInspectionId() { return inspectionId; }
    public void setInspectionId(UUID inspectionId) { this.inspectionId = inspectionId; }
    public String getMethodsJson() { return methodsJson; }
    public void setMethodsJson(String methodsJson) { this.methodsJson = methodsJson; }
    public String getFindingsJson() { return findingsJson; }
    public void setFindingsJson(String findingsJson) { this.findingsJson = findingsJson; }
    public BigDecimal getRecommendedAmount() { return recommendedAmount; }
    public void setRecommendedAmount(BigDecimal recommendedAmount) { this.recommendedAmount = recommendedAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getFraudHintsJson() { return fraudHintsJson; }
    public void setFraudHintsJson(String fraudHintsJson) { this.fraudHintsJson = fraudHintsJson; }
    public UUID getAssessorId() { return assessorId; }
    public void setAssessorId(UUID assessorId) { this.assessorId = assessorId; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
