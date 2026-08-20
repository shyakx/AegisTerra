package com.aegisterra.platform.application.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegisterra.platform.application.settlement.spi.ManualSettlementProvider;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderRequest;
import com.aegisterra.platform.application.settlement.spi.PaymentProviderResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualSettlementProviderTest {

    private final ManualSettlementProvider provider = new ManualSettlementProvider();

    @Test
    void executeSucceedsWithManualReference() {
        PaymentProviderRequest request = new PaymentProviderRequest(
            UUID.randomUUID(), "SET-1", new BigDecimal("1000"), "RWF",
            "MANUAL", "Farmer", "0730000000", null, null, "corr"
        );
        PaymentProviderResult result = provider.execute(request);
        assertTrue(result.success());
        assertTrue(result.providerReference().startsWith("MANUAL-"));
    }

    @Test
    void executeUsesProvidedExternalReference() {
        PaymentProviderRequest request = new PaymentProviderRequest(
            UUID.randomUUID(), "SET-1", new BigDecimal("1000"), "RWF",
            "MANUAL", "Farmer", "0730000000", null, "REF-123", "corr"
        );
        PaymentProviderResult result = provider.execute(request);
        assertEquals("REF-123", result.providerReference());
        assertTrue(result.success());
    }

    @Test
    void validateRejectsNonPositiveAmount() {
        PaymentProviderRequest request = new PaymentProviderRequest(
            UUID.randomUUID(), "SET-1", BigDecimal.ZERO, "RWF",
            "MANUAL", "Farmer", "0730000000", null, null, "corr"
        );
        PaymentProviderResult result = provider.validate(request);
        assertTrue(!result.success());
    }
}
