package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransactionEntity, UUID> {
    Optional<LedgerTransactionEntity> findByIdAndDeletedFalse(UUID id);

    List<LedgerTransactionEntity> findBySettlementIdAndDeletedFalseOrderByPostedAtAsc(UUID settlementId);

    Optional<LedgerTransactionEntity> findByTransactionNumberAndDeletedFalse(String transactionNumber);
}
