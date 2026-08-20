package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimHistoryRepository extends JpaRepository<ClaimHistoryEntity, UUID> {
    List<ClaimHistoryEntity> findByOriginalIdOrderByChangedAtDesc(UUID originalId);
}
