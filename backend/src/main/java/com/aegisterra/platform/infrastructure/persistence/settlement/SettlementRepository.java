package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<SettlementEntity, UUID> {

    Optional<SettlementEntity> findByIdAndDeletedFalse(UUID id);

    Optional<SettlementEntity> findBySettlementNumberAndDeletedFalse(String settlementNumber);

    List<SettlementEntity> findBySourceModuleAndSourceRecordIdAndDeletedFalse(String sourceModule, UUID sourceRecordId);

    Optional<SettlementEntity> findByWorkflowInstanceIdAndDeletedFalse(UUID workflowInstanceId);

        @Query("""
                SELECT s FROM SettlementEntity s
                WHERE s.id = :settlementId AND s.deleted = false AND s.sourceModule = 'CLAIMS'
                    AND EXISTS (
                        SELECT c.id FROM ClaimEntity c
                        WHERE c.id = s.sourceRecordId AND c.farmerId = :farmerId AND c.deleted = false
                    )
                """)
        Optional<SettlementEntity> findByIdForFarmer(
                @Param("settlementId") UUID settlementId,
                @Param("farmerId") UUID farmerId
        );

    List<SettlementEntity> findByStatusInAndDeletedFalse(Collection<String> statuses);

    long countByDeletedFalseAndStatus(String status);

    @Query("""
        SELECT s FROM SettlementEntity s
        WHERE s.deleted = false
          AND s.sourceModule = :sourceModule
          AND s.sourceRecordId = :sourceRecordId
          AND s.status NOT IN ('CANCELLED','FAILED','REJECTED','CLOSED','REVERSED')
        """)
    Optional<SettlementEntity> findActiveBySource(
        @Param("sourceModule") String sourceModule,
        @Param("sourceRecordId") UUID sourceRecordId
    );

    @Query("""
        SELECT s FROM SettlementEntity s
        WHERE s.deleted = false
          AND (:q IS NULL OR :q = '' OR LOWER(s.settlementNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(s.sourceReference, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
               OR LOWER(COALESCE(s.beneficiaryName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
          AND (:status IS NULL OR :status = '' OR s.status = :status)
          AND (:sourceModule IS NULL OR :sourceModule = '' OR s.sourceModule = :sourceModule)
          AND (:providerCode IS NULL OR :providerCode = '' OR s.providerCode = :providerCode)
          AND (:hasFromAmount = false OR s.amount >= :fromAmount)
          AND (:hasToAmount = false OR s.amount <= :toAmount)
          AND (:hasFromDate = false OR s.createdAt >= :fromDate)
          AND (:hasToDate = false OR s.createdAt <= :toDate)
        """)
    Page<SettlementEntity> search(
        @Param("q") String q,
        @Param("status") String status,
        @Param("sourceModule") String sourceModule,
        @Param("providerCode") String providerCode,
        @Param("hasFromAmount") boolean hasFromAmount,
        @Param("fromAmount") BigDecimal fromAmount,
        @Param("hasToAmount") boolean hasToAmount,
        @Param("toAmount") BigDecimal toAmount,
        @Param("hasFromDate") boolean hasFromDate,
        @Param("fromDate") Instant fromDate,
        @Param("hasToDate") boolean hasToDate,
        @Param("toDate") Instant toDate,
        Pageable pageable
    );

        @Query("""
                SELECT s FROM SettlementEntity s
                WHERE s.deleted = false
                    AND (:q IS NULL OR :q = '' OR LOWER(s.settlementNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                             OR LOWER(COALESCE(s.sourceReference, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                             OR LOWER(COALESCE(s.beneficiaryName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
                    AND (:status IS NULL OR :status = '' OR s.status = :status)
                    AND (:sourceModule IS NULL OR :sourceModule = '' OR s.sourceModule = :sourceModule)
                    AND (:providerCode IS NULL OR :providerCode = '' OR s.providerCode = :providerCode)
                    AND (:hasFromAmount = false OR s.amount >= :fromAmount)
                    AND (:hasToAmount = false OR s.amount <= :toAmount)
                    AND (:hasFromDate = false OR s.createdAt >= :fromDate)
                    AND (:hasToDate = false OR s.createdAt <= :toDate)
                    AND s.sourceModule = 'CLAIMS'
                    AND EXISTS (
                        SELECT c.id FROM ClaimEntity c
                        WHERE c.id = s.sourceRecordId AND c.farmerId = :farmerId AND c.deleted = false
                    )
                """)
        Page<SettlementEntity> searchForFarmer(
                @Param("q") String q,
                @Param("status") String status,
                @Param("sourceModule") String sourceModule,
                @Param("providerCode") String providerCode,
                @Param("hasFromAmount") boolean hasFromAmount,
                @Param("fromAmount") BigDecimal fromAmount,
                @Param("hasToAmount") boolean hasToAmount,
                @Param("toAmount") BigDecimal toAmount,
                @Param("hasFromDate") boolean hasFromDate,
                @Param("fromDate") Instant fromDate,
                @Param("hasToDate") boolean hasToDate,
                @Param("toDate") Instant toDate,
                @Param("farmerId") UUID farmerId,
                Pageable pageable
        );

    @Query("""
        SELECT s.status, COUNT(s), COALESCE(SUM(s.amount), 0)
        FROM SettlementEntity s
        WHERE s.deleted = false
        GROUP BY s.status
        """)
    List<Object[]> totalsByStatus();

    @Query("""
        SELECT s.providerCode, COUNT(s), COALESCE(SUM(s.amount), 0)
        FROM SettlementEntity s
        WHERE s.deleted = false AND s.status = 'COMPLETED'
        GROUP BY s.providerCode
        """)
    List<Object[]> completedByProvider();
}
