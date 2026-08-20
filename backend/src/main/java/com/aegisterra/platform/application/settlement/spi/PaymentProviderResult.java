package com.aegisterra.platform.application.settlement.spi;

public record PaymentProviderResult(
    boolean success,
    String status,
    String providerReference,
    String message,
    String rawResponseJson
) {
    public static PaymentProviderResult ok(String status, String providerReference, String message) {
        return new PaymentProviderResult(true, status, providerReference, message, null);
    }

    public static PaymentProviderResult fail(String status, String message) {
        return new PaymentProviderResult(false, status, null, message, null);
    }

    public static PaymentProviderResult stub(String code, String operation) {
        return fail("NOT_IMPLEMENTED", code + " provider does not support " + operation + " in v1");
    }
}
