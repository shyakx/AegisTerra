package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutHistoryRepository extends JpaRepository<PayoutHistoryEntity, UUID> {
    List<PayoutHistoryEntity> findByOriginalIdOrderByChangedAtDesc(UUID originalId);
}
