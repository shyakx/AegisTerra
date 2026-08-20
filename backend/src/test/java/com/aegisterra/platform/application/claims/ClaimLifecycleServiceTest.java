package com.aegisterra.platform.application.claims;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimStatusHistoryRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClaimLifecycleServiceTest {

    private ClaimLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        ClaimRepository claimRepository = mock(ClaimRepository.class);
        ClaimStatusHistoryRepository historyRepository = mock(ClaimStatusHistoryRepository.class);
        ClaimsAuditHelper auditHelper = mock(ClaimsAuditHelper.class);
        EventBus eventBus = mock(EventBus.class);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(claimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lifecycleService = new ClaimLifecycleService(claimRepository, historyRepository, auditHelper, eventBus);
    }

    @Test
    void allowsDraftToSubmitted() {
        ClaimEntity claim = claim(ClaimStatus.DRAFT);
        assertDoesNotThrow(() -> lifecycleService.transition(claim, ClaimStatus.SUBMITTED, UUID.randomUUID(), "ok"));
    }

    @Test
    void rejectsDraftToApproved() {
        ClaimEntity claim = claim(ClaimStatus.DRAFT);
        assertThrows(ResponseStatusException.class,
            () -> lifecycleService.transition(claim, ClaimStatus.APPROVED, UUID.randomUUID(), "bad"));
    }

    @Test
    void rejectsSettledToApproved() {
        ClaimEntity claim = claim(ClaimStatus.SETTLED);
        assertThrows(ResponseStatusException.class,
            () -> lifecycleService.transition(claim, ClaimStatus.APPROVED, UUID.randomUUID(), "bad"));
    }

    private static ClaimEntity claim(ClaimStatus status) {
        ClaimEntity claim = new ClaimEntity();
        claim.setId(UUID.randomUUID());
        claim.setClaimNumber("CLM-2026-00000001");
        claim.setStatus(status.name());
        claim.setCorrelationId(UUID.randomUUID().toString());
        return claim;
    }
}
