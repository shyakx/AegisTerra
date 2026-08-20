package com.aegisterra.platform.application.settlement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementRepository;
import com.aegisterra.platform.infrastructure.persistence.settlement.SettlementStatusHistoryRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SettlementLifecycleServiceTest {

    private SettlementLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        SettlementRepository settlementRepository = mock(SettlementRepository.class);
        SettlementStatusHistoryRepository historyRepository = mock(SettlementStatusHistoryRepository.class);
        SettlementAuditHelper auditHelper = mock(SettlementAuditHelper.class);
        EventBus eventBus = mock(EventBus.class);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lifecycleService = new SettlementLifecycleService(settlementRepository, historyRepository, auditHelper, eventBus);
    }

    @Test
    void allowsPendingToUnderReview() {
        SettlementEntity settlement = settlement(SettlementStatus.PENDING);
        assertDoesNotThrow(() ->
            lifecycleService.transition(settlement, SettlementStatus.UNDER_REVIEW, UUID.randomUUID(), "ok"));
    }

    @Test
    void rejectsPendingToCompleted() {
        SettlementEntity settlement = settlement(SettlementStatus.PENDING);
        assertThrows(ResponseStatusException.class,
            () -> lifecycleService.transition(settlement, SettlementStatus.COMPLETED, UUID.randomUUID(), "bad"));
    }

    @Test
    void rejectsCompletedToSent() {
        SettlementEntity settlement = settlement(SettlementStatus.COMPLETED);
        assertThrows(ResponseStatusException.class,
            () -> lifecycleService.transition(settlement, SettlementStatus.SENT, UUID.randomUUID(), "bad"));
    }

    private static SettlementEntity settlement(SettlementStatus status) {
        SettlementEntity entity = new SettlementEntity();
        entity.setId(UUID.randomUUID());
        entity.setSettlementNumber("SET-2026-000000001");
        entity.setSourceModule("MANUAL");
        entity.setSourceRecordId(UUID.randomUUID());
        entity.setAmount(new BigDecimal("100.00"));
        entity.setCurrency("RWF");
        entity.setStatus(status.name());
        entity.setCorrelationId(UUID.randomUUID().toString());
        return entity;
    }
}
