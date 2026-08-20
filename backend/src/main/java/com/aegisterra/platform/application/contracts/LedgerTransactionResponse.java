package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionResponse(
    UUID id,
    String transactionNumber,
    UUID settlementId,
    String transactionType,
    String description,
    String currency,
    String correlationId,
    Instant postedAt,
    String status,
    List<LedgerEntryResponse> entries
) {}
