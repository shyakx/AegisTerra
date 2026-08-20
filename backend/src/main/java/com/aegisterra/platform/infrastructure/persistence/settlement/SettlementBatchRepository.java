package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatchEntity, UUID> {
}
