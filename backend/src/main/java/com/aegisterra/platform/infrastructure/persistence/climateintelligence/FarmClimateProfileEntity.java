package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farm_climate_profiles")
public class FarmClimateProfileEntity extends AuditableEntity {

    @Column(name = "farm_id", nullable = false)
    private UUID farmId;

    @Column(name = "rule_set_code", length = 64)
    private String ruleSetCode;

    @Column(name = "rule_version", length = 32)
    private String ruleVersion;

    @Column(name = "profile_json", nullable = false, columnDefinition = "text")
    private String profileJson;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public UUID getFarmId() { return farmId; }
    public void setFarmId(UUID farmId) { this.farmId = farmId; }
    public String getRuleSetCode() { return ruleSetCode; }
    public void setRuleSetCode(String ruleSetCode) { this.ruleSetCode = ruleSetCode; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getProfileJson() { return profileJson; }
    public void setProfileJson(String profileJson) { this.profileJson = profileJson; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
