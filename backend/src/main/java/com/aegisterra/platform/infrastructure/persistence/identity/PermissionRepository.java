package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    List<PermissionEntity> findAllByDeletedFalseOrderByCodeAsc();

    List<PermissionEntity> findByDeletedFalseOrderByCodeAsc();

    @Query("""
        select distinct p from PermissionEntity p
        join RolePermissionEntity rp on rp.permissionId = p.id
        join UserRoleEntity ur on ur.roleId = rp.roleId
        where ur.userId = :userId
          and ur.deleted = false
          and rp.deleted = false
          and p.deleted = false
        order by p.code
        """)
    List<PermissionEntity> findActivePermissionsByUserId(@Param("userId") UUID userId);
}
