package com.aegisterra.platform.infrastructure.persistence.insurance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocumentEntity, UUID> {
    List<PolicyDocumentEntity> findByPolicyIdAndDeletedFalseOrderByDocumentTypeAscVersionNoDesc(UUID policyId);
    Optional<PolicyDocumentEntity> findFirstByPolicyIdAndDocumentTypeAndDeletedFalseOrderByVersionNoDesc(UUID policyId, String documentType);
}
