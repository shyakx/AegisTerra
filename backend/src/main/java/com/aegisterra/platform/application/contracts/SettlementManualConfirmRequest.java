package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlementManualConfirmRequest(
    @NotBlank @Size(max = 255) String providerReference,
    @Size(max = 2000) String notes
) {}
