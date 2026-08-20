package com.aegisterra.platform.infrastructure.persistence.workflow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTaskEntity, UUID> {
    Optional<WorkflowTaskEntity> findByIdAndDeletedFalse(UUID id);

    List<WorkflowTaskEntity> findByInstanceIdAndStepCodeAndDeletedFalse(UUID instanceId, String stepCode);

    @Query("""
        select t from WorkflowTaskEntity t
        where t.deleted = false
          and (:status is null or t.status = :status)
          and (:subjectType is null or t.subjectType = :subjectType)
          and (:taskType is null or t.taskType = :taskType)
          and (:assigneeUserId is null or t.assigneeUserId = :assigneeUserId)
          and (:roleCode is null or t.assigneeRoleCode = :roleCode)
          and (:q is null or lower(t.title) like lower(concat('%', cast(:q as string), '%'))
               or lower(t.subjectType) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<WorkflowTaskEntity> search(
        @Param("status") String status,
        @Param("subjectType") String subjectType,
        @Param("taskType") String taskType,
        @Param("assigneeUserId") UUID assigneeUserId,
        @Param("roleCode") String roleCode,
        @Param("q") String q,
        Pageable pageable
    );

    @Query("""
        select t from WorkflowTaskEntity t
        where t.deleted = false
          and t.status in :openStatuses
          and (
            t.assigneeUserId = :userId
            or (t.assigneeUserId is null and t.assigneeRoleCode in :roleCodes and t.status = 'PENDING')
          )
          and (:status is null or t.status = :status)
          and (:subjectType is null or t.subjectType = :subjectType)
          and (:q is null or lower(t.title) like lower(concat('%', cast(:q as string), '%')))
        """)
    Page<WorkflowTaskEntity> searchMyInbox(
        @Param("userId") UUID userId,
        @Param("roleCodes") Collection<String> roleCodes,
        @Param("openStatuses") Collection<String> openStatuses,
        @Param("status") String status,
        @Param("subjectType") String subjectType,
        @Param("q") String q,
        Pageable pageable
    );
}
