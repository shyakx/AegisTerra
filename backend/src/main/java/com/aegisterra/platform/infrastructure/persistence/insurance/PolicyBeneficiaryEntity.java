package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "policy_beneficiaries")
public class PolicyBeneficiaryEntity extends AuditableEntity {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 64)
    private String relationship;

    @Column(name = "national_id", length = 32)
    private String nationalId;

    @Column(name = "share_pct", precision = 7, scale = 4)
    private BigDecimal sharePct;

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public BigDecimal getSharePct() { return sharePct; }
    public void setSharePct(BigDecimal sharePct) { this.sharePct = sharePct; }
}
