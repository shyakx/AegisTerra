package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record RoleView(UUID id, String code, String name, String description, boolean systemRole) {}
