package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionVersionRepository extends JpaRepository<WorkflowDefinitionVersionEntity, UUID> {
    Optional<WorkflowDefinitionVersionEntity> findByIdAndDeletedFalse(UUID id);
    List<WorkflowDefinitionVersionEntity> findByDefinitionIdAndDeletedFalseOrderByVersionNoAsc(UUID definitionId);
    Optional<WorkflowDefinitionVersionEntity> findFirstByDefinitionIdAndDeletedFalseOrderByVersionNoDesc(UUID definitionId);
    Optional<WorkflowDefinitionVersionEntity> findByDefinitionIdAndStatusAndDeletedFalse(UUID definitionId, String status);
}
