package com.aegisterra.platform.infrastructure.persistence.claims;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "claim_types")
public class ClaimTypeEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 32)
    private String nature;

    @Column(name = "workflow_definition_code", nullable = false, length = 64)
    private String workflowDefinitionCode;

    @Column(name = "assessment_profile_json", columnDefinition = "text")
    private String assessmentProfileJson;

    @Column(name = "requires_geo", nullable = false)
    private boolean requiresGeo;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }
    public String getWorkflowDefinitionCode() { return workflowDefinitionCode; }
    public void setWorkflowDefinitionCode(String workflowDefinitionCode) { this.workflowDefinitionCode = workflowDefinitionCode; }
    public String getAssessmentProfileJson() { return assessmentProfileJson; }
    public void setAssessmentProfileJson(String assessmentProfileJson) { this.assessmentProfileJson = assessmentProfileJson; }
    public boolean isRequiresGeo() { return requiresGeo; }
    public void setRequiresGeo(boolean requiresGeo) { this.requiresGeo = requiresGeo; }
}
