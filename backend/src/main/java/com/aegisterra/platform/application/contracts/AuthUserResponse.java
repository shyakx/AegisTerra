package com.aegisterra.platform.application.contracts;

import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String status,
    boolean mustChangePassword,
    List<String> roles,
    List<String> permissions,
    UUID farmerId
) {
}
