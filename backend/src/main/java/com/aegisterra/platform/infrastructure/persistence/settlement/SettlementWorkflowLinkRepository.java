package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementWorkflowLinkRepository extends JpaRepository<SettlementWorkflowLinkEntity, UUID> {
    List<SettlementWorkflowLinkEntity> findBySettlementIdAndDeletedFalse(UUID settlementId);

    Optional<SettlementWorkflowLinkEntity> findByWorkflowInstanceIdAndDeletedFalse(UUID workflowInstanceId);
}
