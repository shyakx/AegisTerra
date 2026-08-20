package com.aegisterra.platform.application.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aegisterra.platform.application.events.EventBus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

class LedgerServiceTest {

    private LedgerService ledgerService;
    private final List<LedgerEntryEntity> savedEntries = new ArrayList<>();

    @BeforeEach
    void setUp() {
        savedEntries.clear();
        LedgerTransactionRepository txRepo = mock(LedgerTransactionRepository.class);
        LedgerEntryRepository entryRepo = mock(LedgerEntryRepository.class);
        SettlementAuditHelper auditHelper = mock(SettlementAuditHelper.class);
        EventBus eventBus = mock(EventBus.class);

        when(txRepo.save(any())).thenAnswer(inv -> {
            LedgerTransactionEntity tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });
        when(entryRepo.save(any())).thenAnswer(inv -> {
            LedgerEntryEntity e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            savedEntries.add(e);
            return e;
        });
        when(entryRepo.findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(any())).thenReturn(List.of());
        when(entryRepo.findByLedgerTransactionIdAndDeletedFalseOrderByEntryNoAsc(any()))
            .thenAnswer(inv -> savedEntries.stream()
                .filter(e -> e.getLedgerTransactionId().equals(inv.getArgument(0)))
                .toList());
        when(txRepo.findBySettlementIdAndDeletedFalseOrderByPostedAtAsc(any())).thenReturn(List.of());

        ledgerService = new LedgerService(txRepo, entryRepo, auditHelper, eventBus);
    }

    @Test
    void postsBalancedDebitAndCredit() {
        SettlementEntity settlement = settlement();
        LedgerTransactionResponse tx = ledgerService.postSettlementCompletion(settlement, UUID.randomUUID());
        assertEquals(2, tx.entries().size());
        BigDecimal debit = tx.entries().stream().filter(e -> "DEBIT".equals(e.entryType()))
            .map(e -> e.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = tx.entries().stream().filter(e -> "CREDIT".equals(e.entryType()))
            .map(e -> e.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, debit.compareTo(credit));
        assertEquals(0, debit.compareTo(settlement.getAmount()));
    }

    @Test
    void listsPersistedEntriesWhenUnfiltered() {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setLedgerTransactionId(UUID.randomUUID());
        entry.setSettlementId(UUID.randomUUID());
        entry.setEntryNo(1);
        entry.setEntryType("DEBIT");
        entry.setAccountCode("PAYABLE.CLAIMS");
        entry.setAmount(new BigDecimal("150000.00"));
        entry.setCurrency("RWF");
        entry.setNarration("Debit payable for SET-2026-001");
        entry.setPostedAt(Instant.parse("2026-04-01T10:00:00Z"));

        LedgerEntryRepository entryRepo = mock(LedgerEntryRepository.class);
        when(entryRepo.findByDeletedFalse(any())).thenReturn(new PageImpl<>(List.of(entry)));
        when(entryRepo.findByIdAndDeletedFalse(entry.getId())).thenReturn(java.util.Optional.of(entry));
        LedgerService svc = new LedgerService(
            mock(LedgerTransactionRepository.class), entryRepo,
            mock(SettlementAuditHelper.class), mock(EventBus.class)
        );

        PageResponse<LedgerEntryResponse> page = svc.list(null, PageRequest.of(0, 20));
        assertEquals(1, page.content().size());
        assertEquals(entry.getId(), page.content().get(0).id());
        assertEquals("PAYABLE.CLAIMS", page.content().get(0).accountCode());

        LedgerEntryResponse loaded = (LedgerEntryResponse) svc.getById(entry.getId());
        assertEquals(entry.getId(), loaded.id());
    }

    @Test
    void rejectsInPlaceMutation() {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setAmount(new BigDecimal("100.00"));
        LedgerEntryRepository entryRepo = mock(LedgerEntryRepository.class);
        when(entryRepo.existsById(entry.getId())).thenReturn(true);
        LedgerService svc = new LedgerService(
            mock(LedgerTransactionRepository.class), entryRepo,
            mock(SettlementAuditHelper.class), mock(EventBus.class)
        );
        assertThrows(ResponseStatusException.class, () -> svc.assertImmutable(entry));
        assertTrue(true);
    }

    private static SettlementEntity settlement() {
        SettlementEntity entity = new SettlementEntity();
        entity.setId(UUID.randomUUID());
        entity.setSettlementNumber("SET-2026-000000002");
        entity.setSourceModule("CLAIMS");
        entity.setSourceRecordId(UUID.randomUUID());
        entity.setAmount(new BigDecimal("150000.00"));
        entity.setCurrency("RWF");
        entity.setProviderCode("MANUAL");
        entity.setCorrelationId(UUID.randomUUID().toString());
        entity.setStatus("CONFIRMED");
        return entity;
    }
}
