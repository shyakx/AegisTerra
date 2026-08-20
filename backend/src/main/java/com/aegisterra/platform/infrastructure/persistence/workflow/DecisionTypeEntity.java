package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "decision_types")
public class DecisionTypeEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment;

    @Column(name = "requires_target_user", nullable = false)
    private boolean requiresTargetUser;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequiresComment() { return requiresComment; }
    public void setRequiresComment(boolean requiresComment) { this.requiresComment = requiresComment; }
    public boolean isRequiresTargetUser() { return requiresTargetUser; }
    public void setRequiresTargetUser(boolean requiresTargetUser) { this.requiresTargetUser = requiresTargetUser; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
