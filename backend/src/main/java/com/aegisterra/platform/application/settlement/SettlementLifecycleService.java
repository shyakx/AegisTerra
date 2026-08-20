package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementStatusHistoryEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementStatusHistoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementLifecycleService {

    private final SettlementRepository settlementRepository;
    private final SettlementStatusHistoryRepository historyRepository;
    private final SettlementAuditHelper auditHelper;
    private final EventBus eventBus;

    public SettlementLifecycleService(
        SettlementRepository settlementRepository,
        SettlementStatusHistoryRepository historyRepository,
        SettlementAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.settlementRepository = settlementRepository;
        this.historyRepository = historyRepository;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional
    public SettlementEntity transition(SettlementEntity settlement, SettlementStatus target, UUID actorId, String reason) {
        SettlementStatus current = SettlementStatus.parse(settlement.getStatus());
        if (current == target) {
            return settlement;
        }
        current.assertCanTransitionTo(target);

        String from = settlement.getStatus();
        settlement.setStatus(target.name());
        settlement.setUpdatedBy(actorId);
        if (reason != null && !reason.isBlank()) {
            settlement.setReasonCode(reason.length() > 64 ? reason.substring(0, 64) : reason);
        }
        if (target == SettlementStatus.COMPLETED || target == SettlementStatus.FAILED
            || target == SettlementStatus.REJECTED || target == SettlementStatus.CANCELLED
            || target == SettlementStatus.CLOSED) {
            if (settlement.getCompletedAt() == null
                && (target == SettlementStatus.COMPLETED || target == SettlementStatus.FAILED
                || target == SettlementStatus.REJECTED || target == SettlementStatus.CANCELLED)) {
                settlement.setCompletedAt(Instant.now());
            }
        }
        if (target == SettlementStatus.FAILED && reason != null) {
            settlement.setFailureReason(reason.length() > 2000 ? reason.substring(0, 2000) : reason);
        }
        settlementRepository.save(settlement);

        SettlementStatusHistoryEntity history = new SettlementStatusHistoryEntity();
        history.setSettlementId(settlement.getId());
        history.setFromStatus(from);
        history.setToStatus(target.name());
        history.setReason(reason);
        history.setActorId(actorId);
        history.setOccurredAt(Instant.now());
        history.setDeleted(false);
        history.setCreatedBy(actorId);
        history.setStatus("ACTIVE");
        historyRepository.save(history);

        auditHelper.record(AuditAction.SETTLEMENT_STATUS_CHANGED, actorId, "settlement", settlement.getId(),
            from, target.name(), reason);

        Map<String, Object> payload = new HashMap<>();
        payload.put("settlementNumber", settlement.getSettlementNumber());
        payload.put("fromStatus", from);
        payload.put("status", target.name());
        payload.put("sourceModule", settlement.getSourceModule());
        payload.put("sourceRecordId", settlement.getSourceRecordId().toString());
        payload.put("reason", reason == null ? "" : reason);
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.SETTLEMENT_STATUS_CHANGED,
            actorId,
            "SETTLEMENT",
            settlement.getId(),
            settlement.getCorrelationId(),
            payload
        ));

        if (target == SettlementStatus.APPROVED) {
            auditHelper.record(AuditAction.SETTLEMENT_APPROVED, actorId, "settlement", settlement.getId(),
                from, target.name(), reason);
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.SETTLEMENT_APPROVED, actorId, "SETTLEMENT", settlement.getId(),
                settlement.getCorrelationId(), payload));
        } else if (target == SettlementStatus.PROCESSING) {
            auditHelper.record(AuditAction.SETTLEMENT_PROCESSING, actorId, "settlement", settlement.getId(),
                from, target.name(), reason);
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.SETTLEMENT_PROCESSING, actorId, "SETTLEMENT", settlement.getId(),
                settlement.getCorrelationId(), payload));
        } else if (target == SettlementStatus.COMPLETED) {
            auditHelper.record(AuditAction.SETTLEMENT_COMPLETED, actorId, "settlement", settlement.getId(),
                from, target.name(), reason);
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.SETTLEMENT_COMPLETED, actorId, "SETTLEMENT", settlement.getId(),
                settlement.getCorrelationId(), payload));
        } else if (target == SettlementStatus.FAILED) {
            auditHelper.record(AuditAction.SETTLEMENT_FAILED, actorId, "settlement", settlement.getId(),
                from, target.name(), reason);
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.SETTLEMENT_FAILED, actorId, "SETTLEMENT", settlement.getId(),
                settlement.getCorrelationId(), payload));
        } else if (target == SettlementStatus.CANCELLED) {
            auditHelper.record(AuditAction.SETTLEMENT_CANCELLED, actorId, "settlement", settlement.getId(),
                from, target.name(), reason);
            eventBus.publish(PlatformDomainEvent.of(
                DomainEventTypes.SETTLEMENT_CANCELLED, actorId, "SETTLEMENT", settlement.getId(),
                settlement.getCorrelationId(), payload));
        }
        return settlement;
    }

    @Transactional(readOnly = true)
    public SettlementEntity require(UUID id) {
        return settlementRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement not found"));
    }
}
