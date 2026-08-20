package com.aegisterra.platform.application.contracts;

import java.time.Instant;

public record ClimateTimelineItemResponse(
    Instant occurredAt,
    String eventType,
    String severity,
    String title,
    String detail,
    String evidenceJson
) {}
