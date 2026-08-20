package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "username is required") String username,
    @NotBlank(message = "password is required") String password,
    Boolean rememberMe
) {
}
