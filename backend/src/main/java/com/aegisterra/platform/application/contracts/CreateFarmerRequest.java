package com.aegisterra.platform.application.contracts;

public record CreateFarmerRequest(
    String firstName,
    String lastName,
    String nationalId,
    String phoneNumber,
    String email,
    String districtId,
    String sectorId,
    String cellId,
    String villageId
) {
}
