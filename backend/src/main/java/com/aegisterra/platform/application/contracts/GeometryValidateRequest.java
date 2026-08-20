package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record GeometryValidateRequest(@NotBlank String geoJson) {}
