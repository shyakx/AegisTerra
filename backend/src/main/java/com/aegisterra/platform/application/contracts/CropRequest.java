package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CropRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 255) String scientificName,
    String status
) {}
