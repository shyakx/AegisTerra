package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CoverageResidualService {

    private static final List<String> CONSUMING_STATUSES = List.of(
        ClaimStatus.APPROVED.name(),
        ClaimStatus.PAYMENT_PENDING.name(),
        ClaimStatus.SETTLED.name()
    );

    private final InsurancePolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    public CoverageResidualService(InsurancePolicyRepository policyRepository, ClaimRepository claimRepository) {
        this.policyRepository = policyRepository;
        this.claimRepository = claimRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal residualForPolicy(UUID policyId) {
        return residualForPolicy(policyId, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal residualForPolicy(UUID policyId, UUID excludeClaimId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndDeletedFalse(policyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
        BigDecimal consumed = claimRepository.findByPolicyIdAndStatusInAndDeletedFalse(policyId, CONSUMING_STATUSES)
            .stream()
            .filter(c -> excludeClaimId == null || !c.getId().equals(excludeClaimId))
            .map(this::amountConsumed)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal residual = policy.getCoverageAmount().subtract(consumed);
        return residual.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : residual;
    }

    private BigDecimal amountConsumed(ClaimEntity claim) {
        if (claim.getApprovedAmount() != null) {
            return claim.getApprovedAmount();
        }
        if (claim.getAssessedAmount() != null) {
            return claim.getAssessedAmount();
        }
        return claim.getClaimedAmount() != null ? claim.getClaimedAmount() : BigDecimal.ZERO;
    }
}
