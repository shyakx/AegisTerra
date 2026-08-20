package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    List<RoleEntity> findAllByDeletedFalseOrderByCodeAsc();

    List<RoleEntity> findByDeletedFalseOrderByCodeAsc();

    Optional<RoleEntity> findByCodeAndDeletedFalse(String code);

    List<RoleEntity> findByCodeInAndDeletedFalse(Collection<String> codes);

    @Query("""
        select r from RoleEntity r
        join UserRoleEntity ur on ur.roleId = r.id
        where ur.userId = :userId and ur.deleted = false and r.deleted = false
        order by r.code
        """)
    List<RoleEntity> findActiveRolesByUserId(@Param("userId") UUID userId);
}
