package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SeasonRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String status
) {}
