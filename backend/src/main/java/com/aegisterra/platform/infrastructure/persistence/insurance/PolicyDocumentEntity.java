package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_documents")
public class PolicyDocumentEntity extends AuditableEntity {

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @Column(name = "content_sha256", nullable = false, length = 128)
    private String contentSha256;

    @Column(name = "qr_payload", length = 1000)
    private String qrPayload;

    @Column(name = "signature_status", nullable = false, length = 32)
    private String signatureStatus;

    @Column(name = "signed_at")
    private Instant signedAt;

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getContentSha256() { return contentSha256; }
    public void setContentSha256(String contentSha256) { this.contentSha256 = contentSha256; }
    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String qrPayload) { this.qrPayload = qrPayload; }
    public String getSignatureStatus() { return signatureStatus; }
    public void setSignatureStatus(String signatureStatus) { this.signatureStatus = signatureStatus; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
}
