package com.aegisterra.platform.infrastructure.persistence.climate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherObservationRepository extends JpaRepository<WeatherObservationEntity, UUID> {
    List<WeatherObservationEntity> findByStationIdAndObservedAtBetweenOrderByObservedAtAsc(
        UUID stationId,
        Instant from,
        Instant to
    );

    long countByObservedAtGreaterThanEqual(Instant from);

    @Query("""
        select o from WeatherObservationEntity o
        where o.observedAt >= :from and o.observedAt <= :to
          and (:stationId is null or o.stationId = :stationId)
          and (:variableCode is null or o.variableCode = :variableCode)
          and (:qualityFlag is null or o.qualityFlag = :qualityFlag)
        """)
    Page<WeatherObservationEntity> search(
        @Param("stationId") UUID stationId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("variableCode") String variableCode,
        @Param("qualityFlag") String qualityFlag,
        Pageable pageable
    );

    @Query("""
        select coalesce(sum(o.value), 0) from WeatherObservationEntity o
        where o.stationId = :stationId
          and o.variableCode = :variable
          and o.qualityFlag in ('VALID', 'SUSPECT')
          and o.observedAt >= :from and o.observedAt <= :to
        """)
    BigDecimal sumVariable(
        @Param("stationId") UUID stationId,
        @Param("variable") String variable,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
