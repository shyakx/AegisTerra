package com.aegisterra.platform.application.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisterra.platform.application.settlement.spi.ManualSettlementProvider;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderRequest;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderResult;
import com.aegisterra.platform.domain.settlement.SettlementStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettlementStatusAndProviderTest {

    @Test
    void settlementNumberFormat() {
        SettlementNumberGenerator gen = new SettlementNumberGenerator();
        String n = gen.next();
        assertTrue(n.matches("SET-\\d{4}-\\d{9}"));
    }

    @Test
    void legalTransitions() {
        SettlementStatus.PENDING.assertCanTransitionTo(SettlementStatus.UNDER_REVIEW);
        SettlementStatus.UNDER_REVIEW.assertCanTransitionTo(SettlementStatus.APPROVED);
        SettlementStatus.APPROVED.assertCanTransitionTo(SettlementStatus.PROCESSING);
        SettlementStatus.PROCESSING.assertCanTransitionTo(SettlementStatus.SENT);
        SettlementStatus.SENT.assertCanTransitionTo(SettlementStatus.CONFIRMED);
        SettlementStatus.CONFIRMED.assertCanTransitionTo(SettlementStatus.COMPLETED);
        SettlementStatus.COMPLETED.assertCanTransitionTo(SettlementStatus.REVERSED);
    }

    @Test
    void illegalTransitionRejected() {
        assertThrows(Exception.class,
            () -> SettlementStatus.PENDING.assertCanTransitionTo(SettlementStatus.COMPLETED));
    }

    @Test
    void manualProviderExecute() {
        ManualSettlementProvider provider = new ManualSettlementProvider();
        PaymentProviderRequest request = new PaymentProviderRequest(
            UUID.randomUUID(),
            "SET-2026-000000001",
            new BigDecimal("1000.00"),
            "RWF",
            "MANUAL",
            "Farmer",
            "MANUAL",
            null,
            "REF-1",
            "corr-1"
        );
        PaymentProviderResult result = provider.execute(request);
        assertTrue(result.success());
        assertEquals("REF-1", result.providerReference());
    }
}
