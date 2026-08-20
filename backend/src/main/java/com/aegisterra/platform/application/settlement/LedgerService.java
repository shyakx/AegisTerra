package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.settlement.LedgerEntryEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.LedgerEntryRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.LedgerTransactionEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.LedgerTransactionRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.application.contracts.LedgerEntryResponse;
import com.aegisterra.platform.application.contracts.LedgerTransactionResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
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
public class LedgerService {

    public static final String ACCOUNT_PAYABLE_CLAIMS = "PAYABLE.CLAIMS";
    public static final String ACCOUNT_CASH_MANUAL = "CASH.MANUAL";

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final SettlementAuditHelper auditHelper;
    private final EventBus eventBus;

    public LedgerService(
        LedgerTransactionRepository transactionRepository,
        LedgerEntryRepository entryRepository,
        SettlementAuditHelper auditHelper,
        EventBus eventBus
    ) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
    }

    @Transactional
    public LedgerTransactionResponse postSettlementCompletion(SettlementEntity settlement, UUID actorId) {
        if (!entryRepository.findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(settlement.getId()).isEmpty()) {
            List<LedgerTransactionEntity> existing = transactionRepository
                .findBySettlementIdAndDeletedFalseOrderByPostedAtAsc(settlement.getId());
            if (!existing.isEmpty()) {
                return toTransactionResponse(existing.get(existing.size() - 1));
            }
        }

        Instant now = Instant.now();
        LedgerTransactionEntity tx = new LedgerTransactionEntity();
        tx.setTransactionNumber(nextTransactionNumber());
        tx.setSettlementId(settlement.getId());
        tx.setTransactionType("DISBURSEMENT");
        tx.setDescription("Settlement completion for " + settlement.getSettlementNumber());
        tx.setCurrency(settlement.getCurrency());
        tx.setCorrelationId(settlement.getCorrelationId());
        tx.setPostedAt(now);
        tx.setDeleted(false);
        tx.setCreatedBy(actorId);
        tx.setStatus("POSTED");
        transactionRepository.save(tx);

        BigDecimal amount = settlement.getAmount();
        String payableAccount = payableAccountFor(settlement.getSourceModule());
        String cashAccount = cashAccountFor(settlement.getProviderCode());

        LedgerEntryEntity debit = createEntry(tx.getId(), settlement, 1, "DEBIT", payableAccount, amount,
            settlement.getCurrency(), "Debit payable for " + settlement.getSettlementNumber(), now, actorId);
        LedgerEntryEntity credit = createEntry(tx.getId(), settlement, 2, "CREDIT", cashAccount, amount,
            settlement.getCurrency(), "Credit cash for " + settlement.getSettlementNumber(), now, actorId);
        entryRepository.save(debit);
        entryRepository.save(credit);

        auditHelper.record(AuditAction.LEDGER_ENTRY_CREATED, actorId, "ledger_transaction", tx.getId(),
            null, tx.getTransactionNumber(), "settlement-complete");

        publishEntryCreated(settlement, debit, actorId);
        publishEntryCreated(settlement, credit, actorId);

        return toTransactionResponse(tx);
    }

    @Transactional
    public LedgerTransactionResponse postReversal(SettlementEntity settlement, UUID actorId, String reason) {
        Instant now = Instant.now();
        LedgerTransactionEntity tx = new LedgerTransactionEntity();
        tx.setTransactionNumber(nextTransactionNumber());
        tx.setSettlementId(settlement.getId());
        tx.setTransactionType("REVERSAL");
        tx.setDescription(reason == null ? "Settlement reversal" : reason);
        tx.setCurrency(settlement.getCurrency());
        tx.setCorrelationId(settlement.getCorrelationId());
        tx.setPostedAt(now);
        tx.setDeleted(false);
        tx.setCreatedBy(actorId);
        tx.setStatus("POSTED");
        transactionRepository.save(tx);

        List<LedgerEntryEntity> originals = entryRepository
            .findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(settlement.getId());
        int entryNo = originals.stream().mapToInt(LedgerEntryEntity::getEntryNo).max().orElse(0) + 1;
        BigDecimal amount = settlement.getAmount();
        String payableAccount = payableAccountFor(settlement.getSourceModule());
        String cashAccount = cashAccountFor(settlement.getProviderCode());

        LedgerEntryEntity debit = createEntry(tx.getId(), settlement, entryNo, "REVERSAL", cashAccount, amount,
            settlement.getCurrency(), "Reversal debit cash", now, actorId);
        LedgerEntryEntity credit = createEntry(tx.getId(), settlement, entryNo + 1, "REVERSAL", payableAccount, amount,
            settlement.getCurrency(), "Reversal credit payable", now, actorId);
        entryRepository.save(debit);
        entryRepository.save(credit);

        auditHelper.record(AuditAction.LEDGER_ENTRY_CREATED, actorId, "ledger_transaction", tx.getId(),
            null, tx.getTransactionNumber(), reason);
        return toTransactionResponse(tx);
    }

    @Transactional
    public LedgerTransactionResponse postAdjustment(
        SettlementEntity settlement, BigDecimal amount, String entryType, String accountCode,
        String narration, UUID actorId
    ) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjustment amount must be positive");
        }
        Instant now = Instant.now();
        LedgerTransactionEntity tx = new LedgerTransactionEntity();
        tx.setTransactionNumber(nextTransactionNumber());
        tx.setSettlementId(settlement.getId());
        tx.setTransactionType("ADJUSTMENT");
        tx.setDescription(narration);
        tx.setCurrency(settlement.getCurrency());
        tx.setCorrelationId(settlement.getCorrelationId());
        tx.setPostedAt(now);
        tx.setDeleted(false);
        tx.setCreatedBy(actorId);
        tx.setStatus("POSTED");
        transactionRepository.save(tx);

        int entryNo = entryRepository.findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(settlement.getId())
            .stream().mapToInt(LedgerEntryEntity::getEntryNo).max().orElse(0) + 1;
        LedgerEntryEntity entry = createEntry(tx.getId(), settlement, entryNo,
            entryType == null ? "ADJUSTMENT" : entryType.toUpperCase(),
            accountCode, amount, settlement.getCurrency(), narration, now, actorId);
        entryRepository.save(entry);
        return toTransactionResponse(tx);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> entriesForSettlement(UUID settlementId) {
        return entryRepository.findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(settlementId).stream()
            .map(this::toEntryResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryResponse> list(UUID settlementId, Pageable pageable) {
        var page = settlementId != null
            ? entryRepository.findBySettlementIdAndDeletedFalse(settlementId, pageable)
            : entryRepository.findByDeletedFalse(pageable);
        return PageResponse.from(page.map(this::toEntryResponse));
    }

    @Transactional(readOnly = true)
    public LedgerTransactionResponse getTransaction(UUID id) {
        LedgerTransactionEntity tx = transactionRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger transaction not found"));
        return toTransactionResponse(tx);
    }

    @Transactional(readOnly = true)
    public Object getById(UUID id) {
        var tx = transactionRepository.findByIdAndDeletedFalse(id);
        if (tx.isPresent()) {
            return toTransactionResponse(tx.get());
        }
        return entryRepository.findByIdAndDeletedFalse(id)
            .map(this::toEntryResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ledger record not found"));
    }

    /**
     * Ledger entries are immutable — callers must never update amounts or account codes in place.
     */
    public void assertImmutable(LedgerEntryEntity existing) {
        if (existing.getId() != null && entryRepository.existsById(existing.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ledger entries are immutable; post an adjustment or reversal instead");
        }
    }

    private LedgerEntryEntity createEntry(
        UUID txId, SettlementEntity settlement, int entryNo, String entryType, String accountCode,
        BigDecimal amount, String currency, String narration, Instant postedAt, UUID actorId
    ) {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setLedgerTransactionId(txId);
        entry.setSettlementId(settlement.getId());
        entry.setEntryNo(entryNo);
        entry.setEntryType(entryType);
        entry.setAccountCode(accountCode);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setNarration(narration);
        entry.setPostedAt(postedAt);
        entry.setDeleted(false);
        entry.setCreatedBy(actorId);
        entry.setStatus("POSTED");
        return entry;
    }

    private void publishEntryCreated(SettlementEntity settlement, LedgerEntryEntity entry, UUID actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("settlementNumber", settlement.getSettlementNumber());
        payload.put("entryType", entry.getEntryType());
        payload.put("accountCode", entry.getAccountCode());
        payload.put("amount", entry.getAmount().toPlainString());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.LEDGER_ENTRY_CREATED,
            actorId,
            "SETTLEMENT",
            settlement.getId(),
            settlement.getCorrelationId(),
            payload
        ));
    }

    private LedgerTransactionResponse toTransactionResponse(LedgerTransactionEntity tx) {
        List<LedgerEntryResponse> entries = entryRepository
            .findByLedgerTransactionIdAndDeletedFalseOrderByEntryNoAsc(tx.getId()).stream()
            .map(this::toEntryResponse)
            .toList();
        return new LedgerTransactionResponse(
            tx.getId(), tx.getTransactionNumber(), tx.getSettlementId(), tx.getTransactionType(),
            tx.getDescription(), tx.getCurrency(), tx.getCorrelationId(), tx.getPostedAt(),
            tx.getStatus(), entries
        );
    }

    private LedgerEntryResponse toEntryResponse(LedgerEntryEntity e) {
        return new LedgerEntryResponse(
            e.getId(), e.getLedgerTransactionId(), e.getSettlementId(), e.getEntryNo(),
            e.getEntryType(), e.getAccountCode(), e.getAmount(), e.getCurrency(),
            e.getNarration(), e.getExternalStatementLineId(), e.getReconciliationRef(), e.getPostedAt()
        );
    }

    private static String payableAccountFor(String sourceModule) {
        if (sourceModule == null) {
            return ACCOUNT_PAYABLE_CLAIMS;
        }
        return switch (sourceModule.toUpperCase()) {
            case "CLAIMS" -> ACCOUNT_PAYABLE_CLAIMS;
            case "SUBSIDY" -> "PAYABLE.SUBSIDY";
            case "PREMIUM_REFUND" -> "PAYABLE.PREMIUM_REFUND";
            default -> "PAYABLE." + sourceModule.toUpperCase();
        };
    }

    private static String cashAccountFor(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return ACCOUNT_CASH_MANUAL;
        }
        return "CASH." + providerCode.toUpperCase();
    }

    private static String nextTransactionNumber() {
        return "LTX-" + Year.now().getValue() + "-"
            + String.format("%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1_000_000_000L));
    }
}
