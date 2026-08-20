package com.aegisterra.platform.infrastructure.security;

import java.util.List;
import java.util.UUID;

public record AegisUserPrincipal(
    UUID id,
    String username,
    String email,
    List<String> roles,
    List<String> permissions
) {}
