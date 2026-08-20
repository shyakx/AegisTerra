package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionTypeRepository extends JpaRepository<DecisionTypeEntity, UUID> {
    List<DecisionTypeEntity> findByDeletedFalseAndStatusOrderBySortOrderAscCodeAsc(String status);

    Optional<DecisionTypeEntity> findByCodeAndDeletedFalse(String code);
}
