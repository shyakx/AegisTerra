package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationDraftRepository extends JpaRepository<RegistrationDraftEntity, UUID> {
    List<RegistrationDraftEntity> findByCreatedByUserIdAndDeletedFalseOrderByUpdatedAtDesc(UUID userId);

    Optional<RegistrationDraftEntity> findByIdAndDeletedFalse(UUID id);
}
