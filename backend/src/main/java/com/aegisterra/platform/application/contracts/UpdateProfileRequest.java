package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Email @Size(max = 320) String email,
    @Size(max = 200) String displayName
) {
}
