package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record PolicyDocumentResponse(
    UUID id,
    UUID policyId,
    String documentType,
    int versionNo,
    String templateCode,
    String contentText,
    String contentSha256,
    String qrPayload,
    String signatureStatus,
    Instant signedAt,
    String status
) {}
