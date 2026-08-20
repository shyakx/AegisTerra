package com.aegisterra.platform.infrastructure.persistence.claims;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_type_document_rules")
public class ClaimTypeDocumentRuleEntity extends AuditableEntity {

    @Column(name = "claim_type_code", nullable = false, length = 64)
    private String claimTypeCode;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(nullable = false)
    private boolean required = true;

    @Column(name = "min_count", nullable = false)
    private int minCount = 1;

    public String getClaimTypeCode() { return claimTypeCode; }
    public void setClaimTypeCode(String claimTypeCode) { this.claimTypeCode = claimTypeCode; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public int getMinCount() { return minCount; }
    public void setMinCount(int minCount) { this.minCount = minCount; }
}
