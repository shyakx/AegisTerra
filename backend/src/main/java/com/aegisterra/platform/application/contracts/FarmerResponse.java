package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record FarmerResponse(
    UUID id,
    String farmerCode,
    UUID householdId,
    String firstName,
    String lastName,
    String nationalId,
    String phoneNumber,
    String email,
    UUID districtId,
    UUID sectorId,
    UUID cellId,
    UUID villageId,
    String status
) {}
