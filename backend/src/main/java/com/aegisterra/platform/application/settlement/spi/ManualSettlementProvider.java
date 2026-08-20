package com.aegisterra.platform.application.settlement.spi;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ManualSettlementProvider implements PaymentProvider {

    public static final String CODE = "MANUAL";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean supports(String paymentMethod) {
        return paymentMethod != null && (
            "MANUAL".equalsIgnoreCase(paymentMethod) || CODE.equalsIgnoreCase(paymentMethod)
        );
    }

    @Override
    public PaymentProviderResult validate(PaymentProviderRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0) {
            return PaymentProviderResult.fail("INVALID", "Amount must be positive");
        }
        if (request.currency() == null || request.currency().isBlank()) {
            return PaymentProviderResult.fail("INVALID", "Currency is required");
        }
        return PaymentProviderResult.ok("VALIDATED", request.providerReference(), "Manual validation passed");
    }

    @Override
    public PaymentProviderResult authorize(PaymentProviderRequest request) {
        PaymentProviderResult validation = validate(request);
        if (!validation.success()) {
            return validation;
        }
        String ref = blankToGenerated(request.providerReference(), "AUTH");
        return PaymentProviderResult.ok("AUTHORIZED", ref, "Manual authorization recorded");
    }

    @Override
    public PaymentProviderResult execute(PaymentProviderRequest request) {
        PaymentProviderResult validation = validate(request);
        if (!validation.success()) {
            return validation;
        }
        String ref = blankToGenerated(
            request.externalReference() != null ? request.externalReference() : request.providerReference(),
            "MANUAL"
        );
        return PaymentProviderResult.ok("SENT", ref, "Manual disbursement marked as sent");
    }

    @Override
    public PaymentProviderResult queryStatus(PaymentProviderRequest request) {
        if (request.providerReference() == null || request.providerReference().isBlank()) {
            return PaymentProviderResult.fail("UNKNOWN", "No provider reference to query");
        }
        return PaymentProviderResult.ok("CONFIRMED", request.providerReference(), "Manual status confirmed");
    }

    @Override
    public PaymentProviderResult cancel(PaymentProviderRequest request) {
        return PaymentProviderResult.ok("CANCELLED", request.providerReference(), "Manual payment cancelled");
    }

    @Override
    public PaymentProviderResult reverse(PaymentProviderRequest request) {
        String ref = blankToGenerated(request.providerReference(), "REV");
        return PaymentProviderResult.ok("REVERSED", ref, "Manual reversal recorded");
    }

    private static String blankToGenerated(String value, String prefix) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
