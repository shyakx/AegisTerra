package com.aegisterra.platform.domain.claims;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ClaimStatus {
    DRAFT,
    SUBMITTED,
    UNDER_VALIDATION,
    ASSIGNED,
    INSPECTION,
    ASSESSMENT,
    PENDING_DECISION,
    RETURNED_FOR_INFO,
    APPROVED,
    REJECTED,
    PAYMENT_PENDING,
    SETTLED,
    CLOSURE_PENDING,
    CANCELLED,
    CLOSED;

    private static final Map<ClaimStatus, Set<ClaimStatus>> TRANSITIONS = Map.ofEntries(
        Map.entry(DRAFT, EnumSet.of(SUBMITTED, CANCELLED)),
        Map.entry(SUBMITTED, EnumSet.of(UNDER_VALIDATION, CANCELLED, RETURNED_FOR_INFO)),
        Map.entry(UNDER_VALIDATION, EnumSet.of(
            ASSIGNED, INSPECTION, ASSESSMENT, PENDING_DECISION, RETURNED_FOR_INFO, REJECTED, CANCELLED
        )),
        Map.entry(ASSIGNED, EnumSet.of(INSPECTION, ASSESSMENT, PENDING_DECISION, RETURNED_FOR_INFO, CANCELLED)),
        Map.entry(INSPECTION, EnumSet.of(ASSESSMENT, RETURNED_FOR_INFO, PENDING_DECISION, CANCELLED)),
        Map.entry(ASSESSMENT, EnumSet.of(PENDING_DECISION, RETURNED_FOR_INFO, CANCELLED)),
        Map.entry(PENDING_DECISION, EnumSet.of(APPROVED, REJECTED, RETURNED_FOR_INFO, CANCELLED)),
        Map.entry(RETURNED_FOR_INFO, EnumSet.of(SUBMITTED, UNDER_VALIDATION, CANCELLED)),
        Map.entry(APPROVED, EnumSet.of(PAYMENT_PENDING, CLOSED)),
        Map.entry(REJECTED, EnumSet.of(CLOSED)),
        Map.entry(PAYMENT_PENDING, EnumSet.of(SETTLED, CLOSED)),
        Map.entry(SETTLED, EnumSet.of(CLOSURE_PENDING, CLOSED)),
        Map.entry(CLOSURE_PENDING, EnumSet.of(CLOSED)),
        Map.entry(CANCELLED, EnumSet.of(CLOSED)),
        Map.entry(CLOSED, EnumSet.noneOf(ClaimStatus.class))
    );

    public static ClaimStatus parse(String value) {
        try {
            return ClaimStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown claim status: " + value);
        }
    }

    public void assertCanTransitionTo(ClaimStatus target) {
        Set<ClaimStatus> allowed = TRANSITIONS.getOrDefault(this, Set.of());
        if (!allowed.contains(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Illegal claim transition from " + this + " to " + target);
        }
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }

    public boolean isSoftTerminal() {
        return this == REJECTED || this == SETTLED || this == CLOSURE_PENDING
            || this == CANCELLED || this == CLOSED;
    }
}
