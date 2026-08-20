package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyBeneficiaryRepository extends JpaRepository<PolicyBeneficiaryEntity, UUID> {
    List<PolicyBeneficiaryEntity> findByPolicyIdAndDeletedFalse(UUID policyId);
}
