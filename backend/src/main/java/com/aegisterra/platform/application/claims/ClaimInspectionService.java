package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimInspectionEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimInspectionRepository;
import com.aegisterra.platform.application.contracts.ClaimInspectionRequest;
import com.aegisterra.platform.application.contracts.ClaimInspectionResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimInspectionService {

    private final ClaimInspectionRepository inspectionRepository;
    private final ClaimLifecycleService lifecycleService;
    private final ClaimsAuditHelper auditHelper;

    public ClaimInspectionService(
        ClaimInspectionRepository inspectionRepository,
        ClaimLifecycleService lifecycleService,
        ClaimsAuditHelper auditHelper
    ) {
        this.inspectionRepository = inspectionRepository;
        this.lifecycleService = lifecycleService;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<ClaimInspectionResponse> list(UUID claimId) {
        lifecycleService.require(claimId);
        return inspectionRepository.findByClaimIdAndDeletedFalseOrderByCreatedAtDesc(claimId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ClaimInspectionResponse scheduleOrComplete(UUID claimId, ClaimInspectionRequest request, UUID actorId) {
        lifecycleService.require(claimId);
        ClaimInspectionEntity entity = new ClaimInspectionEntity();
        entity.setClaimId(claimId);
        entity.setInspectorId(request.inspectorId() == null ? actorId : request.inspectorId());
        entity.setScheduledAt(request.scheduledAt());
        entity.setCheckInLatitude(request.checkInLatitude());
        entity.setCheckInLongitude(request.checkInLongitude());
        entity.setFindingsJson(request.findingsJson());
        entity.setChecklistJson(request.checklistJson());
        entity.setNotes(request.notes());
        boolean complete = request.complete() != null && request.complete();
        if (complete || request.completedAt() != null) {
            entity.setCompletedAt(request.completedAt() == null ? Instant.now() : request.completedAt());
            entity.setStatus("COMPLETED");
        } else {
            entity.setStatus("SCHEDULED");
        }
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        inspectionRepository.save(entity);
        ClaimInspectionResponse response = toResponse(entity);
        auditHelper.record(AuditAction.CLAIM_INSPECTION_RECORDED, actorId, "claim_inspection", entity.getId(), null, response, null);
        return response;
    }

    @Transactional
    public ClaimInspectionResponse complete(UUID claimId, UUID inspectionId, ClaimInspectionRequest request, UUID actorId) {
        lifecycleService.require(claimId);
        ClaimInspectionEntity entity = inspectionRepository.findByIdAndDeletedFalse(inspectionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found"));
        if (!entity.getClaimId().equals(claimId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection not found");
        }
        ClaimInspectionResponse old = toResponse(entity);
        if (request.findingsJson() != null) {
            entity.setFindingsJson(request.findingsJson());
        }
        if (request.checklistJson() != null) {
            entity.setChecklistJson(request.checklistJson());
        }
        if (request.notes() != null) {
            entity.setNotes(request.notes());
        }
        if (request.checkInLatitude() != null) {
            entity.setCheckInLatitude(request.checkInLatitude());
        }
        if (request.checkInLongitude() != null) {
            entity.setCheckInLongitude(request.checkInLongitude());
        }
        entity.setCompletedAt(request.completedAt() == null ? Instant.now() : request.completedAt());
        entity.setStatus("COMPLETED");
        entity.setUpdatedBy(actorId);
        ClaimInspectionResponse response = toResponse(entity);
        auditHelper.record(AuditAction.CLAIM_INSPECTION_RECORDED, actorId, "claim_inspection", inspectionId, old, response, "complete");
        return response;
    }

    private ClaimInspectionResponse toResponse(ClaimInspectionEntity e) {
        return new ClaimInspectionResponse(
            e.getId(), e.getClaimId(), e.getInspectorId(), e.getScheduledAt(), e.getCompletedAt(),
            e.getCheckInLatitude(), e.getCheckInLongitude(), e.getFindingsJson(), e.getChecklistJson(),
            e.getNotes(), e.getStatus(), e.getCreatedAt()
        );
    }
}
