package com.aegisterra.platform.infrastructure.persistence.settlement;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "settlement_workflow_links")
public class SettlementWorkflowLinkEntity extends AuditableEntity {

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;

    @Column(name = "workflow_definition_code", nullable = false, length = 64)
    private String workflowDefinitionCode;

    @Column(name = "link_role", nullable = false, length = 32)
    private String linkRole = "PRIMARY";

    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public UUID getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(UUID workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    public String getWorkflowDefinitionCode() { return workflowDefinitionCode; }
    public void setWorkflowDefinitionCode(String workflowDefinitionCode) { this.workflowDefinitionCode = workflowDefinitionCode; }
    public String getLinkRole() { return linkRole; }
    public void setLinkRole(String linkRole) { this.linkRole = linkRole; }
}
