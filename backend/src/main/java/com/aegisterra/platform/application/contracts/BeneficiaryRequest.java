package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record BeneficiaryRequest(
    @NotBlank String fullName,
    String relationship,
    String nationalId,
    BigDecimal sharePct
) {}
