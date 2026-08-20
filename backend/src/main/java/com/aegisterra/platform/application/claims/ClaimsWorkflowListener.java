package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimDecisionRecordEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimDecisionRecordRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimsWorkflowListener {

    private static final Logger log = LoggerFactory.getLogger(ClaimsWorkflowListener.class);

    private final ClaimRepository claimRepository;
    private final ClaimLifecycleService lifecycleService;
    private final ClaimLifecycleMapper mapper;
    private final ClaimDecisionRecordRepository decisionRecordRepository;
    private final ClaimsAuditHelper auditHelper;
    private final EventBus eventBus;

    public ClaimsWorkflowListener(
        ClaimRepository claimRepository,
        ClaimLifecycleService lifecycleService,
        ClaimLifecycleMapper mapper,
        ClaimDecisionRecordRepository decisionRecordRepository,
        ClaimsAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.claimRepository = claimRepository;
        this.lifecycleService = lifecycleService;
        this.mapper = mapper;
        this.decisionRecordRepository = decisionRecordRepository;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @EventListener
    @Transactional
    public void onDomainEvent(PlatformDomainEvent event) {
        if (!"CLAIM".equalsIgnoreCase(event.subjectType()) || event.subjectId() == null) {
            return;
        }
        try {
            switch (event.eventType()) {
                case DomainEventTypes.WORKFLOW_COMPLETED, DomainEventTypes.WORKFLOW_CANCELLED -> handleTerminal(event);
                case DomainEventTypes.TASK_CREATED -> handleStep(event);
                case DomainEventTypes.DECISION_RECORDED -> handleDecision(event);
                default -> {
                    // ignore
                }
            }
        } catch (Exception ex) {
            log.error("Claims workflow listener failed for event {} ({})", event.eventId(), event.eventType(), ex);
        }
    }

    private void handleTerminal(PlatformDomainEvent event) {
        Optional<ClaimEntity> claimOpt = claimRepository.findByIdAndDeletedFalse(event.subjectId());
        if (claimOpt.isEmpty()) {
            return;
        }
        ClaimEntity claim = claimOpt.get();
        String stepCode = event.payloadString("stepCode");
        Optional<ClaimStatus> terminal = mapper.mapTerminalStep(stepCode);
        if (terminal.isEmpty()) {
            return;
        }
        ClaimStatus target = terminal.get();
        UUID actorId = event.actorId();

        if (target == ClaimStatus.APPROVED) {
            BigDecimal approved = claim.getAssessedAmount() != null ? claim.getAssessedAmount() : claim.getClaimedAmount();
            claim.setApprovedAmount(approved);
            claimRepository.save(claim);
            lifecycleService.transition(claim, ClaimStatus.APPROVED, actorId, "workflow:" + stepCode);
            lifecycleService.transition(claim, ClaimStatus.PAYMENT_PENDING, actorId, "awaiting-settlement");
            auditHelper.record(AuditAction.CLAIM_APPROVED_FOR_PAYMENT, actorId, "claim", claim.getId(),
                null, claim.getApprovedAmount(), null);
            Map<String, Object> payload = new HashMap<>();
            payload.put("claimNumber", claim.getClaimNumber());
            payload.put("approvedAmount", claim.getApprovedAmount() == null ? "" : claim.getApprovedAmount().toPlainString());
            payload.put("currency", claim.getCurrency());
            payload.put("status", claim.getStatus());
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.CLAIM_APPROVED_FOR_PAYMENT,
                actorId,
                "CLAIM",
                claim.getId(),
                claim.getCorrelationId(),
                payload
            ));
            return;
        }

        if (target == ClaimStatus.REJECTED) {
            lifecycleService.transition(claim, ClaimStatus.REJECTED, actorId, "workflow:" + stepCode);
            auditHelper.record(AuditAction.CLAIM_REJECTED, actorId, "claim", claim.getId(), null, claim.getStatus(), stepCode);
            Map<String, Object> payload = new HashMap<>();
            payload.put("claimNumber", claim.getClaimNumber());
            payload.put("reason", stepCode);
            payload.put("status", claim.getStatus());
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.CLAIM_REJECTED,
                actorId,
                "CLAIM",
                claim.getId(),
                claim.getCorrelationId(),
                payload
            ));
            return;
        }

        if (target == ClaimStatus.RETURNED_FOR_INFO) {
            lifecycleService.transition(claim, ClaimStatus.RETURNED_FOR_INFO, actorId, "workflow:" + stepCode);
        }
    }

    private void handleStep(PlatformDomainEvent event) {
        Optional<ClaimEntity> claimOpt = claimRepository.findByIdAndDeletedFalse(event.subjectId());
        if (claimOpt.isEmpty()) {
            return;
        }
        ClaimEntity claim = claimOpt.get();
        String stepCode = event.payloadString("stepCode");
        Optional<ClaimStatus> mapped = mapper.mapWorkflowStep(stepCode);
        if (mapped.isEmpty()) {
            return;
        }
        ClaimStatus current = ClaimStatus.parse(claim.getStatus());
        ClaimStatus target = mapped.get();
        if (current == target || current.isSoftTerminal() || current == ClaimStatus.CLOSED) {
            return;
        }
        try {
            lifecycleService.transition(claim, target, event.actorId(), "workflow-step:" + stepCode);
        } catch (Exception ex) {
            log.debug("Skipping claim step map {} -> {} for claim {}: {}", current, target, claim.getId(), ex.getMessage());
        }
    }

    private void handleDecision(PlatformDomainEvent event) {
        Optional<ClaimEntity> claimOpt = claimRepository.findByIdAndDeletedFalse(event.subjectId());
        if (claimOpt.isEmpty()) {
            return;
        }
        ClaimEntity claim = claimOpt.get();
        ClaimDecisionRecordEntity record = new ClaimDecisionRecordEntity();
        record.setClaimId(claim.getId());
        String decisionId = event.payloadString("decisionId");
        if (decisionId != null && !decisionId.isBlank()) {
            try {
                record.setWorkflowDecisionId(UUID.fromString(decisionId));
            } catch (Exception ignored) {
                // ignore
            }
        }
        String outcome = event.payloadString("outcome");
        record.setOutcomeCode(outcome == null ? "UNKNOWN" : outcome);
        record.setApprovedAmount(claim.getAssessedAmount() != null ? claim.getAssessedAmount() : claim.getClaimedAmount());
        record.setComment(event.payloadString("workflowAction"));
        record.setDecidedBy(event.actorId());
        record.setDecidedAt(Instant.now());
        record.setDeleted(false);
        record.setCreatedBy(event.actorId());
        record.setStatus("ACTIVE");
        decisionRecordRepository.save(record);
    }
}
