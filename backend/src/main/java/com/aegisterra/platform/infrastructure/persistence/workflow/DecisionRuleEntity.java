package com.aegisterra.platform.infrastructure.persistence.workflow;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "decision_rules")
public class DecisionRuleEntity extends AuditableEntity {

    @Column(name = "decision_type_code", nullable = false, length = 64)
    private String decisionTypeCode;

    @Column(name = "outcome_code", nullable = false, length = 64)
    private String outcomeCode;

    @Column(nullable = false, length = 64)
    private String effect;

    @Column(name = "workflow_action", length = 64)
    private String workflowAction;

    @Column(name = "workflow_definition_code", length = 64)
    private String workflowDefinitionCode;

    @Column(name = "step_code", length = 64)
    private String stepCode;

    @Column(name = "task_type", length = 32)
    private String taskType;

    @Column(nullable = false)
    private int priority;

    @Column(length = 2000)
    private String description;

    public String getDecisionTypeCode() { return decisionTypeCode; }
    public void setDecisionTypeCode(String decisionTypeCode) { this.decisionTypeCode = decisionTypeCode; }
    public String getOutcomeCode() { return outcomeCode; }
    public void setOutcomeCode(String outcomeCode) { this.outcomeCode = outcomeCode; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
    public String getWorkflowAction() { return workflowAction; }
    public void setWorkflowAction(String workflowAction) { this.workflowAction = workflowAction; }
    public String getWorkflowDefinitionCode() { return workflowDefinitionCode; }
    public void setWorkflowDefinitionCode(String workflowDefinitionCode) { this.workflowDefinitionCode = workflowDefinitionCode; }
    public String getStepCode() { return stepCode; }
    public void setStepCode(String stepCode) { this.stepCode = stepCode; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
