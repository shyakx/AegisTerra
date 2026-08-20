package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementStatusHistoryRepository extends JpaRepository<SettlementStatusHistoryEntity, UUID> {
    List<SettlementStatusHistoryEntity> findBySettlementIdAndDeletedFalseOrderByOccurredAtAsc(UUID settlementId);
}
