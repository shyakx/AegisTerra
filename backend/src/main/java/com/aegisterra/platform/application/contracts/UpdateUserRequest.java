package com.aegisterra.platform.application.contracts;

import java.util.List;

public record UpdateUserRequest(
    String email,
    String displayName,
    String status,
    List<String> roles
) {}
