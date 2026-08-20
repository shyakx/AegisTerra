package com.aegisterra.platform.application.notification;

import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges domain events to the Communication Engine.
 * Stage 6D uses in-process Spring events (same TX). Kafka/Rabbit consumers will call the same NotificationService.
 */
@Component
public class WorkflowNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNotificationHandler.class);

    private static final Set<String> HANDLED = Set.of(
        DomainEventTypes.WORKFLOW_STARTED,
        DomainEventTypes.WORKFLOW_COMPLETED,
        DomainEventTypes.WORKFLOW_CANCELLED,
        DomainEventTypes.TASK_CREATED,
        DomainEventTypes.TASK_ASSIGNED,
        DomainEventTypes.TASK_CLAIMED,
        DomainEventTypes.TASK_COMPLETED,
        DomainEventTypes.DECISION_RECORDED,
        DomainEventTypes.CLAIM_SUBMITTED,
        DomainEventTypes.CLAIM_STATUS_CHANGED,
        DomainEventTypes.CLAIM_APPROVED_FOR_PAYMENT,
        DomainEventTypes.CLAIM_REJECTED,
        DomainEventTypes.SETTLEMENT_CREATED,
        DomainEventTypes.SETTLEMENT_STATUS_CHANGED,
        DomainEventTypes.SETTLEMENT_APPROVED,
        DomainEventTypes.SETTLEMENT_PROCESSING,
        DomainEventTypes.SETTLEMENT_COMPLETED,
        DomainEventTypes.SETTLEMENT_FAILED,
        DomainEventTypes.SETTLEMENT_CANCELLED,
        DomainEventTypes.LEDGER_ENTRY_CREATED
    );

    private final NotificationService notificationService;

    public WorkflowNotificationHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onDomainEvent(PlatformDomainEvent event) {
        if (!HANDLED.contains(event.eventType())) {
            return;
        }
        try {
            notificationService.notifyFromEvent(event, notificationService.resolveRecipients(event));
        } catch (Exception ex) {
            log.error("Failed to process notification for event {} ({})", event.eventId(), event.eventType(), ex);
        }
    }
}
