package com.aegisterra.platform.application.claims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CoverageResidualServiceTest {

    private InsurancePolicyRepository policyRepository;
    private ClaimRepository claimRepository;
    private CoverageResidualService service;

    @BeforeEach
    void setUp() {
        policyRepository = mock(InsurancePolicyRepository.class);
        claimRepository = mock(ClaimRepository.class);
        service = new CoverageResidualService(policyRepository, claimRepository);
    }

    @Test
    void residualSubtractsApprovedAmounts() {
        UUID policyId = UUID.randomUUID();
        InsurancePolicyEntity policy = new InsurancePolicyEntity();
        policy.setId(policyId);
        policy.setCoverageAmount(new BigDecimal("1000.0000"));
        when(policyRepository.findByIdAndDeletedFalse(policyId)).thenReturn(Optional.of(policy));

        ClaimEntity prior = new ClaimEntity();
        prior.setId(UUID.randomUUID());
        prior.setApprovedAmount(new BigDecimal("250.0000"));
        when(claimRepository.findByPolicyIdAndStatusInAndDeletedFalse(any(), any())).thenReturn(List.of(prior));

        assertEquals(0, new BigDecimal("750.0000").compareTo(service.residualForPolicy(policyId)));
    }

    @Test
    void residualNeverNegative() {
        UUID policyId = UUID.randomUUID();
        InsurancePolicyEntity policy = new InsurancePolicyEntity();
        policy.setId(policyId);
        policy.setCoverageAmount(new BigDecimal("100.0000"));
        when(policyRepository.findByIdAndDeletedFalse(policyId)).thenReturn(Optional.of(policy));

        ClaimEntity prior = new ClaimEntity();
        prior.setId(UUID.randomUUID());
        prior.setApprovedAmount(new BigDecimal("500.0000"));
        when(claimRepository.findByPolicyIdAndStatusInAndDeletedFalse(any(), any())).thenReturn(List.of(prior));

        assertEquals(0, BigDecimal.ZERO.compareTo(service.residualForPolicy(policyId)));
    }
}
