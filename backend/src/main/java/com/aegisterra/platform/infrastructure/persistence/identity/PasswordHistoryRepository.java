package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, UUID> {

    List<PasswordHistoryEntity> findTop5ByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
