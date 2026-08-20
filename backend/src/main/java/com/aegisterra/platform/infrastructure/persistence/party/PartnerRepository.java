package com.aegisterra.platform.infrastructure.persistence.party;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<PartnerEntity, UUID> {
    Optional<PartnerEntity> findByCodeAndDeletedFalse(String code);
}
