package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record InsuranceProductResponse(
    UUID id,
    String code,
    String name,
    String description,
    String pricingStrategyCode,
    String eligibilityJson,
    String configJson,
    String status
) {}
