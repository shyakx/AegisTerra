package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.settlement.spi.PaymentProviderResult;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDecisionRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SettlementWorkflowListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementWorkflowListener.class);

    private final SettlementRepository settlementRepository;
    private final SettlementLifecycleService lifecycleService;
    private final SettlementLifecycleMapper mapper;
    private final SettlementService settlementService;
    private final LedgerService ledgerService;
    private final WorkflowDecisionRepository decisionRepository;

    public SettlementWorkflowListener(
        SettlementRepository settlementRepository,
        SettlementLifecycleService lifecycleService,
        SettlementLifecycleMapper mapper,
        SettlementService settlementService,
        LedgerService ledgerService,
        WorkflowDecisionRepository decisionRepository
    ) {
        this.settlementRepository = settlementRepository;
        this.lifecycleService = lifecycleService;
        this.mapper = mapper;
        this.settlementService = settlementService;
        this.ledgerService = ledgerService;
        this.decisionRepository = decisionRepository;
    }

    @EventListener
    @Transactional
    public void onDomainEvent(PlatformDomainEvent event) {
        if (!"SETTLEMENT".equalsIgnoreCase(event.subjectType()) || event.subjectId() == null) {
            return;
        }
        try {
            switch (event.eventType()) {
                case DomainEventTypes.WORKFLOW_COMPLETED, DomainEventTypes.WORKFLOW_CANCELLED -> handleTerminal(event);
                case DomainEventTypes.TASK_CREATED -> handleStep(event);
                case DomainEventTypes.DECISION_RECORDED, DomainEventTypes.TASK_COMPLETED -> handleDecision(event);
                default -> {
                    // ignore
                }
            }
        } catch (Exception ex) {
            log.error("Settlement workflow listener failed for event {} ({})", event.eventId(), event.eventType(), ex);
        }
    }

    private void handleTerminal(PlatformDomainEvent event) {
        Optional<SettlementEntity> settlementOpt = settlementRepository.findByIdAndDeletedFalse(event.subjectId());
        if (settlementOpt.isEmpty()) {
            return;
        }
        SettlementEntity settlement = settlementOpt.get();
        String stepCode = event.payloadString("stepCode");
        Optional<SettlementStatus> terminal = mapper.mapTerminalStep(stepCode);
        if (terminal.isEmpty()) {
            return;
        }
        SettlementStatus target = terminal.get();
        UUID actorId = event.actorId();

        if (target == SettlementStatus.COMPLETED) {
            SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
            if (current == SettlementStatus.SENT || current == SettlementStatus.PROCESSING) {
                tryTransition(settlement, SettlementStatus.CONFIRMED, actorId, "workflow:" + stepCode);
            }
            current = SettlementStatus.parse(settlement.getStatus());
            if (current == SettlementStatus.CONFIRMED || current == SettlementStatus.SENT) {
                if (current == SettlementStatus.SENT) {
                    tryTransition(settlement, SettlementStatus.CONFIRMED, actorId, "workflow:" + stepCode);
                }
                ledgerService.postSettlementCompletion(settlement, actorId);
                tryTransition(settlement, SettlementStatus.COMPLETED, actorId, "workflow:" + stepCode);
            } else if (current != SettlementStatus.COMPLETED) {
                catchUpToCompleted(settlement, actorId, stepCode);
            }
            return;
        }

        tryTransition(settlement, target, actorId, "workflow:" + stepCode);
    }

    private void catchUpToCompleted(SettlementEntity settlement, UUID actorId, String stepCode) {
        SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
        try {
            if (current == SettlementStatus.APPROVED) {
                lifecycleService.transition(settlement, SettlementStatus.PROCESSING, actorId, "workflow-catchup");
                lifecycleService.transition(settlement, SettlementStatus.SENT, actorId, "workflow-catchup");
                lifecycleService.transition(settlement, SettlementStatus.CONFIRMED, actorId, "workflow-catchup");
            } else if (current == SettlementStatus.PROCESSING) {
                lifecycleService.transition(settlement, SettlementStatus.SENT, actorId, "workflow-catchup");
                lifecycleService.transition(settlement, SettlementStatus.CONFIRMED, actorId, "workflow-catchup");
            }
            ledgerService.postSettlementCompletion(settlement, actorId);
            lifecycleService.transition(settlement, SettlementStatus.COMPLETED, actorId, "workflow:" + stepCode);
        } catch (Exception ex) {
            log.warn("Could not complete settlement {} from {}: {}", settlement.getId(), current, ex.getMessage());
        }
    }

    private void handleStep(PlatformDomainEvent event) {
        Optional<SettlementEntity> settlementOpt = settlementRepository.findByIdAndDeletedFalse(event.subjectId());
        if (settlementOpt.isEmpty()) {
            return;
        }
        SettlementEntity settlement = settlementOpt.get();
        String stepCode = event.payloadString("stepCode");
        Optional<SettlementStatus> mapped = mapper.mapWorkflowStep(stepCode);
        if (mapped.isEmpty()) {
            return;
        }
        SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
        SettlementStatus target = mapped.get();
        if (current == target || current.isSoftTerminal()) {
            return;
        }
        try {
            if (current == SettlementStatus.PENDING && target == SettlementStatus.UNDER_REVIEW) {
                lifecycleService.transition(settlement, target, event.actorId(), "workflow-step:" + stepCode);
                return;
            }
            if (current == SettlementStatus.UNDER_REVIEW && target == SettlementStatus.APPROVED) {
                lifecycleService.transition(settlement, target, event.actorId(), "workflow-step:" + stepCode);
                return;
            }
            if (current == SettlementStatus.APPROVED && target == SettlementStatus.PROCESSING) {
                lifecycleService.transition(settlement, target, event.actorId(), "workflow-step:" + stepCode);
            }
        } catch (Exception ex) {
            log.debug("Skipping settlement step map {} -> {} for {}: {}", current, target, settlement.getId(),
                ex.getMessage());
        }
    }

    private void handleDecision(PlatformDomainEvent event) {
        Optional<SettlementEntity> settlementOpt = settlementRepository.findByIdAndDeletedFalse(event.subjectId());
        if (settlementOpt.isEmpty()) {
            return;
        }
        SettlementEntity settlement = settlementOpt.get();
        String stepCode = resolveStepCode(event);
        String outcome = event.payloadString("decisionType");
        if (outcome == null || outcome.isBlank()) {
            outcome = event.payloadString("outcome");
        }
        if (outcome == null) {
            return;
        }
        String upper = outcome.trim().toUpperCase();
        if (!"APPROVE".equals(upper) && !"APPROVED".equals(upper) && !"COMPLETED".equals(upper)) {
            return;
        }

        if (stepCode != null && "APPROVAL".equalsIgnoreCase(stepCode)) {
            tryTransition(settlement, SettlementStatus.APPROVED, event.actorId(), "approval");
            return;
        }

        if (stepCode != null && "DISBURSE".equalsIgnoreCase(stepCode)) {
            SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
            if (current == SettlementStatus.APPROVED) {
                tryTransition(settlement, SettlementStatus.PROCESSING, event.actorId(), "disburse-start");
            }
            current = SettlementStatus.parse(settlement.getStatus());
            if (current == SettlementStatus.PROCESSING) {
                String externalRef = event.payloadString("providerReference");
                String comment = event.payloadString("comment");
                if (externalRef == null && comment != null && comment.toUpperCase().startsWith("REF:")) {
                    externalRef = comment.substring(4).trim();
                }
                PaymentProviderResult result = settlementService.executeProvider(settlement, event.actorId(), externalRef);
                if (result.success()) {
                    tryTransition(settlement, SettlementStatus.SENT, event.actorId(),
                        "provider:" + result.providerReference());
                } else {
                    tryTransition(settlement, SettlementStatus.FAILED, event.actorId(), result.message());
                }
            }
            return;
        }

        if (stepCode != null && "CONFIRM".equalsIgnoreCase(stepCode)) {
            tryTransition(settlement, SettlementStatus.CONFIRMED, event.actorId(), "confirm");
        }
    }

    private String resolveStepCode(PlatformDomainEvent event) {
        String decisionId = event.payloadString("decisionId");
        if (decisionId != null && !decisionId.isBlank()) {
            try {
                Optional<WorkflowDecisionEntity> decision = decisionRepository.findById(UUID.fromString(decisionId));
                if (decision.isPresent()) {
                    return decision.get().getStepCode();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return event.payloadString("stepCode");
    }

    private void tryTransition(SettlementEntity settlement, SettlementStatus target, UUID actorId, String reason) {
        SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
        if (current == target || current.isSoftTerminal()) {
            return;
        }
        try {
            lifecycleService.transition(settlement, target, actorId, reason);
        } catch (Exception ex) {
            log.debug("Settlement transition {} -> {} skipped: {}", current, target, ex.getMessage());
        }
    }
}
