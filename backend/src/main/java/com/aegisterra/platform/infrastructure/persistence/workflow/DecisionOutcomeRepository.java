package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionOutcomeRepository extends JpaRepository<DecisionOutcomeEntity, UUID> {
    Optional<DecisionOutcomeEntity> findByCodeAndDeletedFalse(String code);
}
