package com.aegisterra.platform.domain.settlement;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum SettlementStatus {
    PENDING,
    UNDER_REVIEW,
    APPROVED,
    PROCESSING,
    SENT,
    CONFIRMED,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED,
    RETRY_PENDING,
    REVERSED,
    CLOSED;

    private static final Map<SettlementStatus, Set<SettlementStatus>> TRANSITIONS = Map.ofEntries(
        Map.entry(PENDING, EnumSet.of(UNDER_REVIEW, CANCELLED)),
        Map.entry(UNDER_REVIEW, EnumSet.of(APPROVED, REJECTED, CANCELLED, RETRY_PENDING)),
        Map.entry(APPROVED, EnumSet.of(PROCESSING, FAILED, CANCELLED)),
        Map.entry(PROCESSING, EnumSet.of(SENT, FAILED, RETRY_PENDING)),
        Map.entry(SENT, EnumSet.of(CONFIRMED, FAILED, RETRY_PENDING)),
        Map.entry(CONFIRMED, EnumSet.of(COMPLETED, FAILED)),
        Map.entry(COMPLETED, EnumSet.of(REVERSED, CLOSED)),
        Map.entry(FAILED, EnumSet.of(RETRY_PENDING, CLOSED)),
        Map.entry(REJECTED, EnumSet.of(CLOSED)),
        Map.entry(CANCELLED, EnumSet.of(CLOSED)),
        Map.entry(RETRY_PENDING, EnumSet.of(UNDER_REVIEW, PROCESSING, CANCELLED)),
        Map.entry(REVERSED, EnumSet.of(CLOSED)),
        Map.entry(CLOSED, EnumSet.noneOf(SettlementStatus.class))
    );

    public static SettlementStatus parse(String value) {
        try {
            return SettlementStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown settlement status: " + value);
        }
    }

    public void assertCanTransitionTo(SettlementStatus target) {
        Set<SettlementStatus> allowed = TRANSITIONS.getOrDefault(this, Set.of());
        if (!allowed.contains(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Illegal settlement transition from " + this + " to " + target);
        }
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }

    public boolean isSoftTerminal() {
        return this == COMPLETED || this == FAILED || this == REJECTED || this == CANCELLED
            || this == REVERSED || this == CLOSED;
    }
}
