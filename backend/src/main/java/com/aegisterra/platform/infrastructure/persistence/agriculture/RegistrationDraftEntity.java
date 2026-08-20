package com.aegisterra.platform.infrastructure.persistence.agriculture;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "registration_drafts")
public class RegistrationDraftEntity extends AuditableEntity {

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "farmer_id")
    private UUID farmerId;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static RegistrationDraftEntity create(UUID userId, String payloadJson, Instant expiresAt) {
        RegistrationDraftEntity draft = new RegistrationDraftEntity();
        draft.setCreatedByUserId(userId);
        draft.setCreatedBy(userId);
        draft.setCurrentStep(1);
        draft.setPayloadJson(payloadJson);
        draft.setExpiresAt(expiresAt);
        draft.setStatus("IN_PROGRESS");
        draft.setDeleted(false);
        return draft;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(UUID farmerId) {
        this.farmerId = farmerId;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
