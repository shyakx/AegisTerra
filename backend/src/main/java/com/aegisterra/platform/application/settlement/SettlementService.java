package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.settlement.spi.PaymentProvider;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderRequest;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderResult;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderLogEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderLogRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementStatusHistoryRepository;
import com.aegisterra.platform.application.contracts.LedgerEntryResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.SettlementCancelRequest;
import com.aegisterra.platform.application.contracts.SettlementCreateRequest;
import com.aegisterra.platform.application.contracts.SettlementManualConfirmRequest;
import com.aegisterra.platform.application.contracts.SettlementResponse;
import com.aegisterra.platform.application.contracts.SettlementTimelineEntryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementStatusHistoryRepository historyRepository;
    private final PaymentProviderLogRepository providerLogRepository;
    private final SettlementLifecycleService lifecycleService;
    private final SettlementIntakeService intakeService;
    private final LedgerService ledgerService;
    private final PaymentProviderRegistry providerRegistry;
    private final SettlementAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public SettlementService(
        SettlementRepository settlementRepository,
        SettlementStatusHistoryRepository historyRepository,
        PaymentProviderLogRepository providerLogRepository,
        SettlementLifecycleService lifecycleService,
        SettlementIntakeService intakeService,
        LedgerService ledgerService,
        PaymentProviderRegistry providerRegistry,
        SettlementAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.settlementRepository = settlementRepository;
        this.historyRepository = historyRepository;
        this.providerLogRepository = providerLogRepository;
        this.lifecycleService = lifecycleService;
        this.intakeService = intakeService;
        this.ledgerService = ledgerService;
        this.providerRegistry = providerRegistry;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> search(
        String q, String status, String sourceModule, String providerCode,
        Instant fromCreated, Instant toCreated, BigDecimal minAmount, BigDecimal maxAmount,
        Pageable pageable
    ) {
        String statusFilter = status == null || status.isBlank() ? null : SettlementStatus.parse(status).name();
        boolean hasFromAmount = minAmount != null;
        boolean hasToAmount = maxAmount != null;
        boolean hasFromDate = fromCreated != null;
        boolean hasToDate = toCreated != null;
        return PageResponse.from(settlementRepository.search(
            blank(q), statusFilter, blank(sourceModule), blank(providerCode),
            hasFromAmount, hasFromAmount ? minAmount : BigDecimal.ZERO,
            hasToAmount, hasToAmount ? maxAmount : BigDecimal.ZERO,
            hasFromDate, hasFromDate ? fromCreated : Instant.EPOCH,
            hasToDate, hasToDate ? toCreated : Instant.EPOCH,
            pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SettlementResponse get(UUID id) {
        return toResponse(lifecycleService.require(id));
    }

    @Transactional(readOnly = true)
    public List<SettlementTimelineEntryResponse> timeline(UUID id) {
        lifecycleService.require(id);
        return historyRepository.findBySettlementIdAndDeletedFalseOrderByOccurredAtAsc(id).stream()
            .map(h -> new SettlementTimelineEntryResponse(
                "STATUS", h.getFromStatus(), h.getToStatus(), h.getReason(), h.getActorId(), h.getOccurredAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> ledger(UUID id) {
        lifecycleService.require(id);
        return ledgerService.entriesForSettlement(id);
    }

    @Transactional
    public SettlementResponse create(SettlementCreateRequest request, UUID actorId) {
        return toResponse(intakeService.createManual(request, actorId));
    }

    @Transactional
    public SettlementResponse manualConfirm(UUID id, SettlementManualConfirmRequest request, UUID actorId) {
        SettlementEntity settlement = lifecycleService.require(id);
        if (!"MANUAL".equalsIgnoreCase(settlement.getProviderCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manual confirm only for MANUAL provider");
        }
        SettlementStatus status = SettlementStatus.parse(settlement.getStatus());
        if (status.isSoftTerminal() && status != SettlementStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement cannot be confirmed in status " + status);
        }

        PaymentProviderResult result = executeProvider(settlement, actorId, request.providerReference());
        if (!result.success()) {
            lifecycleService.transition(settlement, SettlementStatus.FAILED, actorId,
                result.message() == null ? "manual-confirm-failed" : result.message());
            return toResponse(settlement);
        }

        settlement.setProviderReference(result.providerReference());
        settlementRepository.save(settlement);

        if (status == SettlementStatus.UNDER_REVIEW || status == SettlementStatus.PENDING) {
            if (status == SettlementStatus.PENDING) {
                lifecycleService.transition(settlement, SettlementStatus.UNDER_REVIEW, actorId, "manual-confirm");
            }
            lifecycleService.transition(settlement, SettlementStatus.APPROVED, actorId, "manual-approve");
        }
        status = SettlementStatus.parse(settlement.getStatus());
        if (status == SettlementStatus.APPROVED) {
            lifecycleService.transition(settlement, SettlementStatus.PROCESSING, actorId, "manual-process");
        }
        status = SettlementStatus.parse(settlement.getStatus());
        if (status == SettlementStatus.PROCESSING) {
            lifecycleService.transition(settlement, SettlementStatus.SENT, actorId, "manual-sent");
        }
        status = SettlementStatus.parse(settlement.getStatus());
        if (status == SettlementStatus.SENT) {
            lifecycleService.transition(settlement, SettlementStatus.CONFIRMED, actorId, "manual-confirmed");
        }
        status = SettlementStatus.parse(settlement.getStatus());
        if (status == SettlementStatus.CONFIRMED) {
            ledgerService.postSettlementCompletion(settlement, actorId);
            lifecycleService.transition(settlement, SettlementStatus.COMPLETED, actorId, "manual-completed");
        }

        auditHelper.record(AuditAction.SETTLEMENT_COMPLETED, actorId, "settlement", id,
            null, request.providerReference(), request.notes());
        return toResponse(lifecycleService.require(id));
    }

    @Transactional
    public SettlementResponse cancel(UUID id, SettlementCancelRequest request, UUID actorId) {
        SettlementEntity settlement = lifecycleService.require(id);
        SettlementStatus status = SettlementStatus.parse(settlement.getStatus());
        if (status == SettlementStatus.COMPLETED || status == SettlementStatus.CLOSED
            || status == SettlementStatus.REVERSED || status == SettlementStatus.SENT
            || status == SettlementStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel settlement in status " + status);
        }
        settlement.setFailureReason(request.reason());
        settlementRepository.save(settlement);
        lifecycleService.transition(settlement, SettlementStatus.CANCELLED, actorId, request.reason());
        auditHelper.record(AuditAction.SETTLEMENT_CANCELLED, actorId, "settlement", id, null, status.name(), request.reason());
        return toResponse(settlement);
    }

    @Transactional
    public SettlementResponse cancel(UUID id, UUID actorId, String reason) {
        return cancel(id, new SettlementCancelRequest(reason), actorId);
    }

    @Transactional
    public SettlementResponse reverse(UUID id, String reason, UUID actorId) {
        SettlementEntity settlement = lifecycleService.require(id);
        if (SettlementStatus.parse(settlement.getStatus()) != SettlementStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only COMPLETED settlements can be reversed");
        }
        ledgerService.postReversal(settlement, actorId, reason);
        lifecycleService.transition(settlement, SettlementStatus.REVERSED, actorId, reason);
        return toResponse(settlement);
    }

    @Transactional
    public PaymentProviderResult executeProvider(SettlementEntity settlement, UUID actorId, String externalRef) {
        PaymentProvider provider = providerRegistry.require(settlement.getProviderCode());
        PaymentProviderRequest request = new PaymentProviderRequest(
            settlement.getId(),
            settlement.getSettlementNumber(),
            settlement.getAmount(),
            settlement.getCurrency(),
            settlement.getPaymentMethod(),
            settlement.getBeneficiaryName(),
            settlement.getBeneficiaryAccount(),
            settlement.getProviderReference(),
            externalRef,
            settlement.getCorrelationId()
        );
        PaymentProviderResult validate = provider.validate(request);
        logProvider(settlement, "VALIDATE", request, validate, actorId);
        if (!validate.success()) {
            return validate;
        }
        PaymentProviderResult authorize = provider.authorize(request);
        logProvider(settlement, "AUTHORIZE", request, authorize, actorId);
        if (!authorize.success()) {
            return authorize;
        }
        PaymentProviderResult execute = provider.execute(request);
        logProvider(settlement, "EXECUTE", request, execute, actorId);
        if (execute.success() && execute.providerReference() != null) {
            settlement.setProviderReference(execute.providerReference());
            settlementRepository.save(settlement);
        }
        return execute;
    }

    public SettlementResponse toResponse(SettlementEntity s) {
        return new SettlementResponse(
            s.getId(), s.getSettlementNumber(), s.getSourceModule(), s.getSourceRecordId(),
            s.getSourceReference(), s.getAmount(), s.getCurrency(), s.getExchangeRate(),
            s.getPaymentMethod(), s.getProviderCode(), s.getProviderReference(),
            s.getBeneficiaryName(), s.getBeneficiaryAccount(), s.getFinancialSnapshotJson(),
            s.getWorkflowInstanceId(), s.getWorkflowDefinitionCode(), s.getCorrelationId(),
            s.getReasonCode(), s.getFailureReason(), s.getSubmittedAt(), s.getCompletedAt(),
            s.getStatus(), s.getCreatedAt(), s.getUpdatedAt()
        );
    }

    private void logProvider(
        SettlementEntity settlement, String operation, PaymentProviderRequest request,
        PaymentProviderResult result, UUID actorId
    ) {
        PaymentProviderLogEntity log = new PaymentProviderLogEntity();
        log.setSettlementId(settlement.getId());
        log.setProviderCode(settlement.getProviderCode());
        log.setOperation(operation);
        try {
            log.setRequestJson(objectMapper.writeValueAsString(request));
            log.setResponseJson(objectMapper.writeValueAsString(result));
        } catch (Exception ex) {
            log.setRequestJson("{}");
            log.setResponseJson("{}");
        }
        log.setProviderReference(result.providerReference());
        log.setSuccess(result.success());
        log.setErrorMessage(result.success() ? null : result.message());
        log.setOccurredAt(Instant.now());
        log.setDeleted(false);
        log.setCreatedBy(actorId);
        log.setStatus("ACTIVE");
        providerLogRepository.save(log);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
