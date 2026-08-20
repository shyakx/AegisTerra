package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDraftRepository extends JpaRepository<ClaimDraftEntity, UUID> {
    List<ClaimDraftEntity> findByOwnerUserIdAndDeletedFalse(UUID ownerUserId);

    Optional<ClaimDraftEntity> findByIdAndDeletedFalse(UUID id);
}
