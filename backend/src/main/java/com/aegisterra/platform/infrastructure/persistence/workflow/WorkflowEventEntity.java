package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_events")
public class WorkflowEventEntity extends AuditableEntity {

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "from_step_code", length = 64)
    private String fromStepCode;

    @Column(name = "to_step_code", length = 64)
    private String toStepCode;

    @Column(name = "action_code", length = 64)
    private String actionCode;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(length = 2000)
    private String message;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getFromStepCode() { return fromStepCode; }
    public void setFromStepCode(String fromStepCode) { this.fromStepCode = fromStepCode; }
    public String getToStepCode() { return toStepCode; }
    public void setToStepCode(String toStepCode) { this.toStepCode = toStepCode; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
