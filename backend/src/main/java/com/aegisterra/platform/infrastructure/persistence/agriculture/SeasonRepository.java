package com.aegisterra.platform.infrastructure.persistence.agriculture;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<SeasonEntity, UUID> {
    Optional<SeasonEntity> findByCodeAndDeletedFalse(String code);
}
