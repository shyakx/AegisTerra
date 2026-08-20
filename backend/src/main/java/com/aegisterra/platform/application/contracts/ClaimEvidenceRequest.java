package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimEvidenceRequest(
    @NotBlank String documentType,
    @NotBlank String title,
    UUID documentId,
    String storageUri,
    String contentSha256,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant capturedAt,
    String source
) {}
