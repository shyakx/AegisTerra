package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record CoverageLimitResponse(UUID id, String perilCode, BigDecimal limitAmount, String currency) {}
