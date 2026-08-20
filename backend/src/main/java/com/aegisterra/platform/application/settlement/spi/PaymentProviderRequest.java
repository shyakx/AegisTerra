package com.aegisterra.platform.application.settlement.spi;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProviderRequest(
    UUID settlementId,
    String settlementNumber,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String beneficiaryName,
    String beneficiaryAccount,
    String providerReference,
    String externalReference,
    String correlationId
) {}
