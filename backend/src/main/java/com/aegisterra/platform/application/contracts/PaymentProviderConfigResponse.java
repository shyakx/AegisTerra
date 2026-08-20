package com.aegisterra.platform.application.contracts;

public record PaymentProviderConfigResponse(
    java.util.UUID id,
    String providerCode,
    String displayName,
    String paymentMethod,
    boolean enabled,
    String configJson,
    String status
) {}
