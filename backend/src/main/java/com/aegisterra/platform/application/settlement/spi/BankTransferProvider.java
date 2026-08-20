package com.aegisterra.platform.application.settlement.spi;

import org.springframework.stereotype.Component;

@Component
public class BankTransferProvider implements PaymentProvider {

    public static final String CODE = "BANK_TRANSFER";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean supports(String paymentMethod) {
        return "BANK_TRANSFER".equalsIgnoreCase(paymentMethod);
    }

    @Override
    public PaymentProviderResult validate(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "validate");
    }

    @Override
    public PaymentProviderResult authorize(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "authorize");
    }

    @Override
    public PaymentProviderResult execute(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "execute");
    }

    @Override
    public PaymentProviderResult queryStatus(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "queryStatus");
    }

    @Override
    public PaymentProviderResult cancel(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "cancel");
    }

    @Override
    public PaymentProviderResult reverse(PaymentProviderRequest request) {
        return PaymentProviderResult.stub(CODE, "reverse");
    }
}
