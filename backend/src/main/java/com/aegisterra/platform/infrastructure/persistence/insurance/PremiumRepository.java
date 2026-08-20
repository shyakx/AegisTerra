package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumRepository extends JpaRepository<PremiumEntity, UUID> {
    List<PremiumEntity> findByPolicyIdAndDeletedFalse(UUID policyId);
}
