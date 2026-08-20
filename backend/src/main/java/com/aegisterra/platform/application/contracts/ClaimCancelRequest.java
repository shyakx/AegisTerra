package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record ClaimCancelRequest(
    @NotBlank String reason
) {}
