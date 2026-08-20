package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PolicySubmitRequest(
    @NotNull UUID farmerId,
    @NotNull UUID farmId,
    @NotNull UUID productId,
    @NotNull UUID policyTypeId,
    @NotNull UUID coveragePackageId,
    @NotNull UUID premiumQuoteId,
    UUID cropId,
    UUID seasonId,
    UUID cropSeasonId,
    UUID insuranceCompanyId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    List<BeneficiaryRequest> beneficiaries,
    String reason
) {}
