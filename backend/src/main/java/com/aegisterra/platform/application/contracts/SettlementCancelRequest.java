package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlementCancelRequest(
    @NotBlank @Size(max = 2000) String reason
) {}
