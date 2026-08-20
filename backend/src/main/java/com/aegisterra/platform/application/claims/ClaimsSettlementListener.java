package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Option A bridge: on SettlementCompleted for CLAIMS source, close the claim lifecycle.
 * Contains no payment/provider logic. Runs after settlement TX commits.
 */
@Component
public class ClaimsSettlementListener {

    private static final Logger log = LoggerFactory.getLogger(ClaimsSettlementListener.class);

    private final ClaimRepository claimRepository;
    private final ClaimLifecycleService lifecycleService;

    public ClaimsSettlementListener(ClaimRepository claimRepository, ClaimLifecycleService lifecycleService) {
        this.claimRepository = claimRepository;
        this.lifecycleService = lifecycleService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(PlatformDomainEvent event) {
        if (!DomainEventTypes.SETTLEMENT_COMPLETED.equals(event.eventType())
            && !DomainEventTypes.SETTLEMENT_FAILED.equals(event.eventType())) {
            return;
        }
        String sourceModule = event.payloadString("sourceModule");
        if (sourceModule == null || !"CLAIMS".equalsIgnoreCase(sourceModule)) {
            return;
        }
        String sourceRecordId = event.payloadString("sourceRecordId");
        if (sourceRecordId == null || sourceRecordId.isBlank()) {
            return;
        }
        try {
            UUID claimId = UUID.fromString(sourceRecordId);
            Optional<ClaimEntity> claimOpt = claimRepository.findByIdAndDeletedFalse(claimId);
            if (claimOpt.isEmpty()) {
                return;
            }
            ClaimEntity claim = claimOpt.get();
            ClaimStatus current = ClaimStatus.parse(claim.getStatus());
            UUID actorId = event.actorId();

            if (DomainEventTypes.SETTLEMENT_FAILED.equals(event.eventType())) {
                log.info("Settlement failed for claim {}; leaving claim in {}", claimId, current);
                return;
            }

            if (current == ClaimStatus.PAYMENT_PENDING) {
                lifecycleService.transition(claim, ClaimStatus.SETTLED, actorId, "settlement-completed");
                current = ClaimStatus.parse(claim.getStatus());
            }
            if (current == ClaimStatus.SETTLED) {
                lifecycleService.transition(claim, ClaimStatus.CLOSURE_PENDING, actorId, "settlement-closure");
                current = ClaimStatus.parse(claim.getStatus());
            }
            if (current == ClaimStatus.CLOSURE_PENDING) {
                lifecycleService.transition(claim, ClaimStatus.CLOSED, actorId, "settlement-closed");
            }
        } catch (Exception ex) {
            log.error("ClaimsSettlementListener failed for event {}", event.eventId(), ex);
        }
    }
}
