package com.aegisterra.platform.domain.identity;

import java.util.UUID;

/**
 * Fixed UUIDs for seeded system roles (see Liquibase 002-identity-seed).
 */
public final class SystemRoleIds {

    public static final UUID SYSTEM_ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111001");
    public static final UUID INSURANCE_ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111002");
    public static final UUID INSURANCE_OFFICER = UUID.fromString("11111111-1111-1111-1111-111111111003");
    public static final UUID FI_OFFICER = UUID.fromString("11111111-1111-1111-1111-111111111004");
    public static final UUID GOVERNMENT_ANALYST = UUID.fromString("11111111-1111-1111-1111-111111111005");
    public static final UUID AGGREGATOR = UUID.fromString("11111111-1111-1111-1111-111111111006");
    public static final UUID FARMER = UUID.fromString("11111111-1111-1111-1111-111111111007");
    public static final UUID AUDITOR = UUID.fromString("11111111-1111-1111-1111-111111111008");
    public static final UUID SUPPORT = UUID.fromString("11111111-1111-1111-1111-111111111009");

    public static final String SYSTEM_ADMIN_CODE = "SYSTEM_ADMIN";

    private SystemRoleIds() {
    }
}
