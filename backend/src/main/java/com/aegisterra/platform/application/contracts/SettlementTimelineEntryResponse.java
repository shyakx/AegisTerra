package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record SettlementTimelineEntryResponse(
    String kind,
    String fromStatus,
    String toStatus,
    String reason,
    UUID actorId,
    Instant occurredAt
) {}
