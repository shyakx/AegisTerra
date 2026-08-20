package com.aegisterra.platform.infrastructure.persistence.climate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherStationRepository extends JpaRepository<WeatherStationEntity, UUID> {
    Optional<WeatherStationEntity> findByCodeAndDeletedFalse(String code);

    Optional<WeatherStationEntity> findByIdAndDeletedFalse(UUID id);

    long countByDeletedFalse();

    List<WeatherStationEntity> findByDeletedFalse();

    @Query("""
        select s from WeatherStationEntity s
        where s.deleted = false
          and (:q is null or lower(s.code) like lower(concat('%', cast(:q as string), '%'))
               or lower(s.name) like lower(concat('%', cast(:q as string), '%')))
          and (:status is null or s.status = :status)
        """)
    Page<WeatherStationEntity> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
