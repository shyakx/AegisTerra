package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {
    Optional<LedgerEntryEntity> findByIdAndDeletedFalse(UUID id);

    List<LedgerEntryEntity> findBySettlementIdAndDeletedFalseOrderByEntryNoAsc(UUID settlementId);

    List<LedgerEntryEntity> findByLedgerTransactionIdAndDeletedFalseOrderByEntryNoAsc(UUID ledgerTransactionId);

    Page<LedgerEntryEntity> findByDeletedFalse(Pageable pageable);

    Page<LedgerEntryEntity> findBySettlementIdAndDeletedFalse(UUID settlementId, Pageable pageable);
}
