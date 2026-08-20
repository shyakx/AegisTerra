package com.aegisterra.platform.application.contracts;

import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String role,
    List<String> roles,
    String status
) {}
