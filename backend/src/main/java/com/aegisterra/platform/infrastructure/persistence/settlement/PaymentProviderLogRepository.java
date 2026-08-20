package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProviderLogRepository extends JpaRepository<PaymentProviderLogEntity, UUID> {
    List<PaymentProviderLogEntity> findBySettlementIdAndDeletedFalseOrderByOccurredAtAsc(UUID settlementId);
}
