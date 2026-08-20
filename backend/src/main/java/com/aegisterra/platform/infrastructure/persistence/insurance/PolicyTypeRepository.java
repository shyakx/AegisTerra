package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyTypeRepository extends JpaRepository<PolicyTypeEntity, UUID> {
    Optional<PolicyTypeEntity> findByCodeAndDeletedFalse(String code);
}
