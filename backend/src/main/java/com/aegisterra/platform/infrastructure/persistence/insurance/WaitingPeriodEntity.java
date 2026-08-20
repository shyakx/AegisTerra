package com.aegisterra.platform.infrastructure.persistence.insurance;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "waiting_periods")
public class WaitingPeriodEntity extends AuditableEntity {

    @Column(name = "coverage_package_id")
    private UUID coveragePackageId;

    @Column(name = "policy_id")
    private UUID policyId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private int days;

    @Column(length = 1000)
    private String description;

    public UUID getCoveragePackageId() { return coveragePackageId; }
    public void setCoveragePackageId(UUID coveragePackageId) { this.coveragePackageId = coveragePackageId; }
    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
