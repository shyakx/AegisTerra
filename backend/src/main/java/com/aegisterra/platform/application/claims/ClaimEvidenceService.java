package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimEvidenceEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimEvidenceRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.application.contracts.ClaimEvidenceRequest;
import com.aegisterra.platform.application.contracts.ClaimEvidenceResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimEvidenceService {

    private final ClaimEvidenceRepository evidenceRepository;
    private final ClaimLifecycleService lifecycleService;
    private final ClaimsAuditHelper auditHelper;

    public ClaimEvidenceService(
        ClaimEvidenceRepository evidenceRepository,
        ClaimLifecycleService lifecycleService,
        ClaimsAuditHelper auditHelper
    ) {
        this.evidenceRepository = evidenceRepository;
        this.lifecycleService = lifecycleService;
        this.auditHelper = auditHelper;
    }

    @Transactional(readOnly = true)
    public List<ClaimEvidenceResponse> list(UUID claimId) {
        lifecycleService.require(claimId);
        return evidenceRepository.findByClaimIdAndDeletedFalseOrderByCreatedAtDesc(claimId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ClaimEvidenceResponse add(UUID claimId, ClaimEvidenceRequest request, UUID actorId) {
        ClaimEntity claim = lifecycleService.require(claimId);
        ClaimEvidenceEntity evidence = new ClaimEvidenceEntity();
        evidence.setClaimId(claim.getId());
        evidence.setDocumentType(request.documentType().trim().toUpperCase());
        evidence.setTitle(request.title().trim());
        evidence.setDocumentId(request.documentId());
        evidence.setStorageUri(request.storageUri());
        evidence.setContentSha256(request.contentSha256());
        evidence.setLatitude(request.latitude());
        evidence.setLongitude(request.longitude());
        evidence.setCapturedAt(request.capturedAt());
        evidence.setSource(request.source() == null ? "MANUAL" : request.source());
        evidence.setStatus("ACTIVE");
        evidence.setDeleted(false);
        evidence.setCreatedBy(actorId);
        evidenceRepository.save(evidence);
        auditHelper.record(AuditAction.CLAIM_EVIDENCE_ADDED, actorId, "claim_evidence", evidence.getId(),
            null, toResponse(evidence), null);
        return toResponse(evidence);
    }

    @Transactional
    public void softDelete(UUID claimId, UUID evidenceId, UUID actorId) {
        remove(claimId, evidenceId, actorId);
    }

    @Transactional
    public void remove(UUID claimId, UUID evidenceId, UUID actorId) {
        lifecycleService.require(claimId);
        ClaimEvidenceEntity evidence = evidenceRepository.findByIdAndDeletedFalse(evidenceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));
        if (!evidence.getClaimId().equals(claimId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found");
        }
        evidence.setDeleted(true);
        evidence.setUpdatedBy(actorId);
        evidenceRepository.save(evidence);
        auditHelper.record(AuditAction.CLAIM_EVIDENCE_REMOVED, actorId, "claim_evidence", evidenceId, null, null, null);
    }

    private ClaimEvidenceResponse toResponse(ClaimEvidenceEntity e) {
        return new ClaimEvidenceResponse(
            e.getId(), e.getClaimId(), e.getDocumentType(), e.getTitle(), e.getDocumentId(),
            e.getStorageUri(), e.getContentSha256(), e.getLatitude(), e.getLongitude(),
            e.getCapturedAt(), e.getSource(), e.getStatus(), e.getCreatedAt()
        );
    }
}
