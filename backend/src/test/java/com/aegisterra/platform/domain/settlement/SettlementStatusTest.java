package com.aegisterra.platform.domain.settlement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SettlementStatusTest {

    @Test
    void allowsPendingToUnderReview() {
        assertDoesNotThrow(() -> SettlementStatus.PENDING.assertCanTransitionTo(SettlementStatus.UNDER_REVIEW));
    }

    @Test
    void rejectsPendingToCompleted() {
        assertThrows(ResponseStatusException.class,
            () -> SettlementStatus.PENDING.assertCanTransitionTo(SettlementStatus.COMPLETED));
    }

    @Test
    void allowsConfirmedToCompleted() {
        assertDoesNotThrow(() -> SettlementStatus.CONFIRMED.assertCanTransitionTo(SettlementStatus.COMPLETED));
    }

    @Test
    void rejectsCompletedToSent() {
        assertThrows(ResponseStatusException.class,
            () -> SettlementStatus.COMPLETED.assertCanTransitionTo(SettlementStatus.SENT));
    }
}
