package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record PaymentProviderResponse(
    UUID id,
    String providerCode,
    String displayName,
    String paymentMethod,
    boolean enabled,
    String configJson,
    String status
) {}
