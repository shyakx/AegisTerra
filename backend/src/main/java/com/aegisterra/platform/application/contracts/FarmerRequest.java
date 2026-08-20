package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FarmerRequest(
    UUID householdId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Size(max = 32) String nationalId,
    @NotBlank @Size(max = 50) String phoneNumber,
    @Email @Size(max = 255) String email,
    UUID districtId,
    UUID sectorId,
    UUID cellId,
    UUID villageId,
    String status,
    String reason
) {}
