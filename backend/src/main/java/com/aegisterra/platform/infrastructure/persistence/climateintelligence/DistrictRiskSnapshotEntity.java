package com.aegisterra.platform.infrastructure.persistence.climateintelligence;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "district_risk_snapshots")
public class DistrictRiskSnapshotEntity extends AuditableEntity {

    @Column(name = "district_code", nullable = false, length = 64)
    private String districtCode;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal score;

    @Column(length = 32)
    private String grade;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "farm_count", nullable = false)
    private int farmCount;

    @Column(name = "open_alert_count", nullable = false)
    private int openAlertCount;

    @Column(name = "components_json", columnDefinition = "text")
    private String componentsJson;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;

    @Column(name = "rule_set_code", length = 64)
    private String ruleSetCode;

    @Column(name = "rule_version", length = 32)
    private String ruleVersion;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public int getFarmCount() { return farmCount; }
    public void setFarmCount(int farmCount) { this.farmCount = farmCount; }
    public int getOpenAlertCount() { return openAlertCount; }
    public void setOpenAlertCount(int openAlertCount) { this.openAlertCount = openAlertCount; }
    public String getComponentsJson() { return componentsJson; }
    public void setComponentsJson(String componentsJson) { this.componentsJson = componentsJson; }
    public Instant getWindowStart() { return windowStart; }
    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public Instant getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Instant windowEnd) { this.windowEnd = windowEnd; }
    public String getRuleSetCode() { return ruleSetCode; }
    public void setRuleSetCode(String ruleSetCode) { this.ruleSetCode = ruleSetCode; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
