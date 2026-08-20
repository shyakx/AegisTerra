package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record WaitingPeriodResponse(UUID id, String code, int days, String description) {}
