package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumPricingRuleRepository extends JpaRepository<PremiumPricingRuleEntity, UUID> {
    List<PremiumPricingRuleEntity> findByProductIdAndDeletedFalseAndStatusOrderByPriorityAsc(UUID productId, String status);
    List<PremiumPricingRuleEntity> findByDeletedFalseAndStatusOrderByPriorityAsc(String status);
}
