package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID id,
    UUID ledgerTransactionId,
    UUID settlementId,
    int entryNo,
    String entryType,
    String accountCode,
    BigDecimal amount,
    String currency,
    String narration,
    String externalStatementLineId,
    String reconciliationRef,
    Instant postedAt
) {}
