package com.aegisterra.platform.infrastructure.persistence.claims;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimTypeRepository extends JpaRepository<ClaimTypeEntity, UUID> {
    Optional<ClaimTypeEntity> findByCodeAndDeletedFalse(String code);

    List<ClaimTypeEntity> findByDeletedFalseOrderByCodeAsc();
}
