package com.aegisterra.platform.infrastructure.persistence.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsernameIgnoreCaseAndDeletedFalse(String username);

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    List<UserEntity> findAllByDeletedFalseOrderByUsernameAsc();

    List<UserEntity> findByDeletedFalseOrderByUsernameAsc();

    boolean existsByUsernameIgnoreCaseAndDeletedFalse(String username);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    @Query("""
        select count(u) > 0 from UserEntity u
        join UserRoleEntity ur on ur.userId = u.id
        where ur.roleId = :roleId and ur.deleted = false and u.deleted = false
        """)
    boolean existsActiveUserWithRole(@Param("roleId") UUID roleId);

    @Query("""
        select distinct u.id from UserEntity u
        join UserRoleEntity ur on ur.userId = u.id
        join RoleEntity r on r.id = ur.roleId
        where r.code = :roleCode
          and u.deleted = false and ur.deleted = false and r.deleted = false
        """)
    List<UUID> findActiveUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
