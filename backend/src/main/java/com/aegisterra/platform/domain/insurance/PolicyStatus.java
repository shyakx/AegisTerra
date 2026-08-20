package com.aegisterra.platform.domain.insurance;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum PolicyStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    PREMIUM_PENDING,
    ACTIVE,
    SUSPENDED,
    EXPIRED,
    CANCELLED,
    REJECTED;

    private static final Map<PolicyStatus, Set<PolicyStatus>> TRANSITIONS = Map.ofEntries(
        Map.entry(DRAFT, EnumSet.of(SUBMITTED, CANCELLED)),
        Map.entry(SUBMITTED, EnumSet.of(UNDER_REVIEW, CANCELLED)),
        Map.entry(UNDER_REVIEW, EnumSet.of(APPROVED, REJECTED, CANCELLED)),
        Map.entry(APPROVED, EnumSet.of(PREMIUM_PENDING, CANCELLED)),
        Map.entry(PREMIUM_PENDING, EnumSet.of(ACTIVE, CANCELLED, EXPIRED)),
        Map.entry(ACTIVE, EnumSet.of(SUSPENDED, EXPIRED, CANCELLED)),
        Map.entry(SUSPENDED, EnumSet.of(ACTIVE, CANCELLED, EXPIRED)),
        Map.entry(EXPIRED, EnumSet.noneOf(PolicyStatus.class)),
        Map.entry(CANCELLED, EnumSet.noneOf(PolicyStatus.class)),
        Map.entry(REJECTED, EnumSet.noneOf(PolicyStatus.class))
    );

    public static PolicyStatus parse(String value) {
        try {
            return PolicyStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown policy status: " + value);
        }
    }

    public void assertCanTransitionTo(PolicyStatus target) {
        Set<PolicyStatus> allowed = TRANSITIONS.getOrDefault(this, Set.of());
        if (!allowed.contains(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Illegal policy transition from " + this + " to " + target);
        }
    }

    public boolean isTerminal() {
        return this == EXPIRED || this == CANCELLED || this == REJECTED;
    }
}
