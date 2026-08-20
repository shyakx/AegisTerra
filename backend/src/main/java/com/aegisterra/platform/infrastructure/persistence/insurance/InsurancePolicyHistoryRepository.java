package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePolicyHistoryRepository extends JpaRepository<InsurancePolicyHistoryEntity, UUID> {
    List<InsurancePolicyHistoryEntity> findByOriginalIdOrderByChangedAtDesc(UUID originalId);
}
