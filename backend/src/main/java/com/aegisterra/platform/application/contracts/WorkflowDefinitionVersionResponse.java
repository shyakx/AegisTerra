package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record WorkflowDefinitionVersionResponse(
    UUID id,
    UUID definitionId,
    int versionNo,
    String graphJson,
    String status,
    Instant publishedAt,
    UUID publishedBy
) {}
