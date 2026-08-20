package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimStatusHistoryEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimStatusHistoryRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimLifecycleService {

    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final ClaimsAuditHelper auditHelper;
    private final EventBus eventBus;

    public ClaimLifecycleService(
        ClaimRepository claimRepository,
        ClaimStatusHistoryRepository historyRepository,
        ClaimsAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.claimRepository = claimRepository;
        this.historyRepository = historyRepository;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional
    public ClaimEntity transition(ClaimEntity claim, ClaimStatus target, UUID actorId, String reason) {
        ClaimStatus current = ClaimStatus.parse(claim.getStatus());
        if (current == target) {
            return claim;
        }
        current.assertCanTransitionTo(target);

        String from = claim.getStatus();
        claim.setStatus(target.name());
        claim.setUpdatedBy(actorId);
        if (reason != null && !reason.isBlank()) {
            claim.setReasonCode(reason.length() > 64 ? reason.substring(0, 64) : reason);
        }
        if (target == ClaimStatus.CLOSED || target == ClaimStatus.CANCELLED || target == ClaimStatus.REJECTED) {
            claim.setClosedAt(Instant.now());
        }
        claimRepository.save(claim);

        ClaimStatusHistoryEntity history = new ClaimStatusHistoryEntity();
        history.setClaimId(claim.getId());
        history.setFromStatus(from);
        history.setToStatus(target.name());
        history.setReason(reason);
        history.setActorId(actorId);
        history.setOccurredAt(Instant.now());
        history.setDeleted(false);
        history.setCreatedBy(actorId);
        history.setStatus("ACTIVE");
        historyRepository.save(history);

        auditHelper.record(AuditAction.CLAIM_STATUS_CHANGED, actorId, "claim", claim.getId(), from, target.name(), reason);

        Map<String, Object> payload = new HashMap<>();
        payload.put("claimNumber", claim.getClaimNumber());
        payload.put("fromStatus", from);
        payload.put("status", target.name());
        payload.put("reason", reason == null ? "" : reason);
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.CLAIM_STATUS_CHANGED,
            actorId,
            "CLAIM",
            claim.getId(),
            claim.getCorrelationId(),
            payload
        ));
        return claim;
    }

    @Transactional(readOnly = true)
    public ClaimEntity require(UUID id) {
        return claimRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
    }
}
