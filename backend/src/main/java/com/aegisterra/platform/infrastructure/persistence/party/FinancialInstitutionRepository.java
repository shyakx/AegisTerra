package com.aegisterra.platform.infrastructure.persistence.party;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialInstitutionRepository extends JpaRepository<FinancialInstitutionEntity, UUID> {
    Optional<FinancialInstitutionEntity> findByCodeAndDeletedFalse(String code);
}
