package com.aegisterra.platform.application.identity;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.identity.AuditLogEntity;
import com.aegisterra.platform.infrastructure.persistence.identity.AuditLogRepository;
import com.aegisterra.platform.shared.logging.CorrelationIdFilter;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(
        AuditAction action,
        UUID actorUserId,
        String resourceType,
        String resourceId,
        String ipAddress,
        String userAgent,
        String detailsJson
    ) {
        record(action.name(), actorUserId, resourceType, resourceId, ipAddress, userAgent, detailsJson);
    }

    @Transactional
    public void record(
        String action,
        UUID actorUserId,
        String resourceType,
        String resourceId,
        String ipAddress,
        String userAgent,
        String detailsJson
    ) {
        AuditLogEntity log = new AuditLogEntity();
        log.setAction(action);
        log.setActorUserId(actorUserId);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(truncate(userAgent, 512));
        log.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        log.setDetailsJson(detailsJson);
        auditLogRepository.save(log);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
