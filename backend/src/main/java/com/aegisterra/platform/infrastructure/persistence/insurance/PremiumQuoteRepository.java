package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumQuoteRepository extends JpaRepository<PremiumQuoteEntity, UUID> {
    Optional<PremiumQuoteEntity> findByIdAndDeletedFalse(UUID id);
}
