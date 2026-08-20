package com.aegisterra.platform.application.settlement.spi;

public interface PaymentProvider {

    String code();

    boolean supports(String paymentMethod);

    PaymentProviderResult validate(PaymentProviderRequest request);

    PaymentProviderResult authorize(PaymentProviderRequest request);

    PaymentProviderResult execute(PaymentProviderRequest request);

    PaymentProviderResult queryStatus(PaymentProviderRequest request);

    PaymentProviderResult cancel(PaymentProviderRequest request);

    PaymentProviderResult reverse(PaymentProviderRequest request);
}
