package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record ExclusionResponse(UUID id, String code, String description) {}
