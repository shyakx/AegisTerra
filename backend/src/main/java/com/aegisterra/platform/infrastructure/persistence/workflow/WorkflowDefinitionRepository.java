package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {
    Optional<WorkflowDefinitionEntity> findByIdAndDeletedFalse(UUID id);
    Optional<WorkflowDefinitionEntity> findByCodeAndDeletedFalse(String code);
    List<WorkflowDefinitionEntity> findByDeletedFalseOrderByCodeAsc();
}
