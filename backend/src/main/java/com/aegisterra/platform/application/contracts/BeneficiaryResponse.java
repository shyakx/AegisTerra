package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record BeneficiaryResponse(
    UUID id,
    UUID policyId,
    String fullName,
    String relationship,
    String nationalId,
    BigDecimal sharePct
) {}
