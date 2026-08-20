package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowDefinitionResponse(
    UUID id,
    String code,
    String name,
    String description,
    UUID publishedVersionId,
    Integer publishedVersionNo,
    String status,
    List<WorkflowDefinitionVersionResponse> versions
) {}
