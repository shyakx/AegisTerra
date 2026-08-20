package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_decisions")
public class WorkflowDecisionEntity extends AuditableEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "step_code", nullable = false, length = 64)
    private String stepCode;

    @Column(name = "decision_type_code", nullable = false, length = 64)
    private String decisionTypeCode;

    @Column(name = "outcome_code", nullable = false, length = 64)
    private String outcomeCode;

    @Column(nullable = false, length = 64)
    private String effect;

    @Column(name = "workflow_action", length = 64)
    private String workflowAction;

    @Column(name = "comment_text", length = 4000)
    private String commentText;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "subject_type", nullable = false, length = 64)
    private String subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public String getStepCode() { return stepCode; }
    public void setStepCode(String stepCode) { this.stepCode = stepCode; }
    public String getDecisionTypeCode() { return decisionTypeCode; }
    public void setDecisionTypeCode(String decisionTypeCode) { this.decisionTypeCode = decisionTypeCode; }
    public String getOutcomeCode() { return outcomeCode; }
    public void setOutcomeCode(String outcomeCode) { this.outcomeCode = outcomeCode; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
    public String getWorkflowAction() { return workflowAction; }
    public void setWorkflowAction(String workflowAction) { this.workflowAction = workflowAction; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID subjectId) { this.subjectId = subjectId; }
    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
