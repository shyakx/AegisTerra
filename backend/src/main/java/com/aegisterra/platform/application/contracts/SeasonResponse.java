package com.aegisterra.platform.application.contracts;

import java.time.LocalDate;
import java.util.UUID;

public record SeasonResponse(
    UUID id,
    String code,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    String status
) {}
