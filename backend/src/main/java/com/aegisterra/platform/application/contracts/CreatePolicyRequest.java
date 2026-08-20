package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePolicyRequest(
    UUID farmerId,
    UUID farmId,
    UUID policyTypeId,
    BigDecimal premiumAmount,
    BigDecimal coverageAmount,
    String currency
) {
}
