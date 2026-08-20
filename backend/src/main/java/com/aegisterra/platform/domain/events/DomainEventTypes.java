package com.aegisterra.platform.domain.events;

public final class DomainEventTypes {
    public static final String WORKFLOW_STARTED = "WorkflowStarted";
    public static final String WORKFLOW_COMPLETED = "WorkflowCompleted";
    public static final String WORKFLOW_CANCELLED = "WorkflowCancelled";
    public static final String TASK_CREATED = "TaskCreated";
    public static final String TASK_ASSIGNED = "TaskAssigned";
    public static final String TASK_CLAIMED = "TaskClaimed";
    public static final String TASK_COMPLETED = "TaskCompleted";
    public static final String DECISION_RECORDED = "DecisionRecorded";
    public static final String NOTIFICATION_REQUESTED = "NotificationRequested";
    public static final String NOTIFICATION_DELIVERED = "NotificationDelivered";
    public static final String NOTIFICATION_FAILED = "NotificationFailed";
    public static final String CLAIM_SUBMITTED = "ClaimSubmitted";
    public static final String CLAIM_STATUS_CHANGED = "ClaimStatusChanged";
    public static final String CLAIM_APPROVED_FOR_PAYMENT = "ClaimApprovedForPayment";
    public static final String CLAIM_REJECTED = "ClaimRejected";
    public static final String SETTLEMENT_CREATED = "SettlementCreated";
    public static final String SETTLEMENT_STATUS_CHANGED = "SettlementStatusChanged";
    public static final String SETTLEMENT_APPROVED = "SettlementApproved";
    public static final String SETTLEMENT_PROCESSING = "SettlementProcessing";
    public static final String SETTLEMENT_COMPLETED = "SettlementCompleted";
    public static final String SETTLEMENT_FAILED = "SettlementFailed";
    public static final String SETTLEMENT_CANCELLED = "SettlementCancelled";
    public static final String LEDGER_ENTRY_CREATED = "LedgerEntryCreated";
    public static final String CLIMATE_OBSERVATION_ACCEPTED = "ClimateObservationAccepted";
    public static final String CLIMATE_IMPORT_COMPLETED = "ClimateImportCompleted";
    public static final String CLIMATE_QUALITY_REPORT_GENERATED = "ClimateQualityReportGenerated";
    public static final String FARM_RISK_SCORE_CALCULATED = "FarmRiskScoreCalculated";
    public static final String CLIMATE_ALERT_RAISED = "ClimateAlertRaised";
    public static final String CLIMATE_ALERT_RESOLVED = "ClimateAlertResolved";
    public static final String SEASON_SUMMARY_PUBLISHED = "SeasonSummaryPublished";

    private DomainEventTypes() {}
}
