package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(
    @NotBlank String body,
    String visibility
) {}
