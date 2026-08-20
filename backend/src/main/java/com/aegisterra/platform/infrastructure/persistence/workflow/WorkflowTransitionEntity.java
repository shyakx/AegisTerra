package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransitionEntity extends AuditableEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "from_step_code", nullable = false, length = 64)
    private String fromStepCode;

    @Column(name = "to_step_code", nullable = false, length = 64)
    private String toStepCode;

    @Column(name = "action_code", nullable = false, length = 64)
    private String actionCode;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public String getFromStepCode() { return fromStepCode; }
    public void setFromStepCode(String fromStepCode) { this.fromStepCode = fromStepCode; }
    public String getToStepCode() { return toStepCode; }
    public void setToStepCode(String toStepCode) { this.toStepCode = toStepCode; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
