package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Creates settlements after ClaimApprovedForPayment commits — must not share the claim adjudication TX.
 */
@Component
public class SettlementIntakeListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementIntakeListener.class);

    private final SettlementIntakeService intakeService;

    public SettlementIntakeListener(SettlementIntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(PlatformDomainEvent event) {
        if (!DomainEventTypes.CLAIM_APPROVED_FOR_PAYMENT.equals(event.eventType())) {
            return;
        }
        if (!"CLAIM".equalsIgnoreCase(event.subjectType()) || event.subjectId() == null) {
            return;
        }
        try {
            intakeService.intakeFromClaimApproved(event.subjectId(), event.actorId(), event.correlationId());
        } catch (Exception ex) {
            log.error("Settlement intake failed for claim {} event {}", event.subjectId(), event.eventId(), ex);
        }
    }
}
