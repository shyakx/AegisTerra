package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "climate_rule_sets")
public class ClimateRuleSetEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "weights_json", nullable = false, columnDefinition = "text")
    private String weightsJson;

    @Column(name = "thresholds_json", columnDefinition = "text")
    private String thresholdsJson;

    @Column(nullable = false)
    private boolean active;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWeightsJson() { return weightsJson; }
    public void setWeightsJson(String weightsJson) { this.weightsJson = weightsJson; }
    public String getThresholdsJson() { return thresholdsJson; }
    public void setThresholdsJson(String thresholdsJson) { this.thresholdsJson = thresholdsJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
