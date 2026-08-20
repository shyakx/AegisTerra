package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    List<UserRoleEntity> findByUserIdAndDeletedFalse(UUID userId);

    Optional<UserRoleEntity> findByUserIdAndRoleIdAndDeletedFalse(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleIdAndDeletedFalse(UUID userId, UUID roleId);

    @Modifying(clearAutomatically = true)
    @Query("delete from UserRoleEntity ur where ur.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("update UserRoleEntity ur set ur.deleted = true, ur.status = 'DISABLED' where ur.userId = :userId and ur.deleted = false")
    void softDeleteAllByUserId(@Param("userId") UUID userId);
}
