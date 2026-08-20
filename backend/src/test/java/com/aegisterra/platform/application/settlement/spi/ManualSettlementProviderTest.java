package com.aegisterra.platform.application.settlement.spi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualSettlementProviderTest {

    private final ManualSettlementProvider provider = new ManualSettlementProvider();

    @Test
    void validateRejectsNonPositiveAmount() {
        PaymentProviderResult result = provider.validate(request(BigDecimal.ZERO));
        assertFalse(result.success());
    }

    @Test
    void executeSucceedsWithReference() {
        PaymentProviderResult result = provider.execute(request(new BigDecimal("100.00")));
        assertTrue(result.success());
        assertTrue(result.providerReference() != null && !result.providerReference().isBlank());
    }

    @Test
    void stubsDoNotSucceed() {
        assertFalse(new BankTransferProvider().execute(request(BigDecimal.ONE)).success());
        assertFalse(new MobileMoneyProvider().execute(request(BigDecimal.ONE)).success());
        assertFalse(new GovernmentTreasuryProvider().execute(request(BigDecimal.ONE)).success());
        assertFalse(new PartnerSettlementProvider().execute(request(BigDecimal.ONE)).success());
    }

    private static PaymentProviderRequest request(BigDecimal amount) {
        return new PaymentProviderRequest(
            UUID.randomUUID(),
            "SET-2026-000000001",
            amount,
            "RWF",
            "MANUAL",
            "Test Beneficiary",
            "ACC-1",
            null,
            "EXT-1",
            "corr-1"
        );
    }
}
