package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimEvidenceResponse(
    UUID id,
    UUID claimId,
    String documentType,
    String title,
    UUID documentId,
    String storageUri,
    String contentSha256,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant capturedAt,
    String source,
    String status,
    Instant createdAt
) {}
