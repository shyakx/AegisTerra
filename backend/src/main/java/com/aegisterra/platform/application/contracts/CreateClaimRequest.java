package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateClaimRequest(
    UUID policyId,
    String claimType,
    String description,
    BigDecimal claimedAmount,
    String status
) {
}
