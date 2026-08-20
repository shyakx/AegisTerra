package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record TaskCommentResponse(
    UUID id,
    UUID authorId,
    String body,
    String visibility,
    Instant createdAt
) {}
