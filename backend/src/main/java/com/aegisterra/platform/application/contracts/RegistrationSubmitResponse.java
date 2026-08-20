package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record RegistrationSubmitResponse(
    UUID draftId,
    UUID farmerId,
    UUID farmId,
    String status
) {}
