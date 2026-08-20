package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record CropResponse(
    UUID id,
    String code,
    String name,
    String scientificName,
    String status
) {}
