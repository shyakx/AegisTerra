package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record PermissionView(UUID id, String code, String name, String resource, String action) {}
