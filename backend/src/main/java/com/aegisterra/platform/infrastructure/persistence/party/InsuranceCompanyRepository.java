package com.aegisterra.platform.infrastructure.persistence.party;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceCompanyRepository extends JpaRepository<InsuranceCompanyEntity, UUID> {
    Optional<InsuranceCompanyEntity> findByCodeAndDeletedFalse(String code);
}
