package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "published_version_id")
    private UUID publishedVersionId;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getPublishedVersionId() { return publishedVersionId; }
    public void setPublishedVersionId(UUID publishedVersionId) { this.publishedVersionId = publishedVersionId; }
}
