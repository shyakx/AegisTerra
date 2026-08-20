package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyIssuanceDraftRepository extends JpaRepository<PolicyIssuanceDraftEntity, UUID> {
    Optional<PolicyIssuanceDraftEntity> findByIdAndDeletedFalse(UUID id);
    List<PolicyIssuanceDraftEntity> findByCreatedByUserIdAndDeletedFalseOrderByUpdatedAtDesc(UUID userId);
}
