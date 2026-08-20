package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHashAndDeletedFalse(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshTokenEntity t
        set t.status = 'REVOKED'
        where t.familyId = :familyId and t.status = 'ACTIVE'
        """)
    int revokeFamily(@Param("familyId") UUID familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshTokenEntity t
        set t.status = 'REVOKED'
        where t.userId = :userId and t.status = 'ACTIVE'
        """)
    int revokeAllActiveForUser(@Param("userId") UUID userId);
}
