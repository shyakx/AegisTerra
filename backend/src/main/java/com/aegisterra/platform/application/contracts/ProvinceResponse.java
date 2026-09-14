package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record ProvinceResponse(
    UUID id,
    String code,
    String name,
    String status
) {}
