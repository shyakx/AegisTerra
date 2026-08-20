package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, UUID> {

    Optional<LoginSessionEntity> findByIdAndDeletedFalse(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update LoginSessionEntity s
        set s.status = 'REVOKED'
        where s.familyId = :familyId and s.status = 'ACTIVE'
        """)
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update LoginSessionEntity s
        set s.status = 'REVOKED'
        where s.userId = :userId and s.status = 'ACTIVE'
        """)
    int revokeAllActiveForUser(@Param("userId") UUID userId);
}
