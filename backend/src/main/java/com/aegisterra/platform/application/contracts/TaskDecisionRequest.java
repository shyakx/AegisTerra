package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record TaskDecisionRequest(
    @NotBlank String decisionTypeCode,
    String comment,
    UUID targetUserId
) {}
