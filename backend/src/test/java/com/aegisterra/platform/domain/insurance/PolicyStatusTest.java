package com.aegisterra.platform.domain.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyStatusTest {

    @Test
    void allowsLegalTransition() {
        assertThatCode(() -> PolicyStatus.DRAFT.assertCanTransitionTo(PolicyStatus.SUBMITTED))
            .doesNotThrowAnyException();
        assertThatCode(() -> PolicyStatus.PREMIUM_PENDING.assertCanTransitionTo(PolicyStatus.ACTIVE))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsIllegalTransition() {
        assertThatThrownBy(() -> PolicyStatus.DRAFT.assertCanTransitionTo(PolicyStatus.ACTIVE))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Illegal policy transition");
    }
}
