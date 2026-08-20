package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimAssessmentEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimAssessmentRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.application.contracts.ClaimAssessmentRequest;
import com.aegisterra.platform.application.contracts.ClaimAssessmentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimAssessmentService {

    private final ClaimAssessmentRepository assessmentRepository;
    private final ClaimRepository claimRepository;
    private final ClaimLifecycleService lifecycleService;
    private final CoverageResidualService residualService;
    private final ClaimsAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public ClaimAssessmentService(
        ClaimAssessmentRepository assessmentRepository,
        ClaimRepository claimRepository,
        ClaimLifecycleService lifecycleService,
        CoverageResidualService residualService,
        ClaimsAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.assessmentRepository = assessmentRepository;
        this.claimRepository = claimRepository;
        this.lifecycleService = lifecycleService;
        this.residualService = residualService;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ClaimAssessmentResponse> list(UUID claimId) {
        lifecycleService.require(claimId);
        return assessmentRepository.findByClaimIdAndDeletedFalse(claimId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ClaimAssessmentResponse create(UUID claimId, ClaimAssessmentRequest request, UUID actorId) {
        ClaimEntity claim = lifecycleService.require(claimId);
        BigDecimal residual = residualService.residualForPolicy(claim.getPolicyId(), claimId);
        if (request.recommendedAmount().compareTo(residual) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Recommended amount exceeds coverage residual (" + residual + ")");
        }
        if (request.recommendedAmount().compareTo(claim.getClaimedAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Recommended amount cannot exceed claimed amount");
        }

        ClaimAssessmentEntity entity = new ClaimAssessmentEntity();
        entity.setClaimId(claimId);
        entity.setMethodsJson(request.methodsJson());
        entity.setFindingsJson(request.findingsJson());
        entity.setRecommendedAmount(request.recommendedAmount());
        entity.setAssessedAmount(request.recommendedAmount());
        entity.setCurrency(request.currency() == null || request.currency().isBlank() ? claim.getCurrency() : request.currency());
        if (!entity.getCurrency().equalsIgnoreCase(claim.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Assessment currency must match claim currency");
        }
        entity.setConfidence(request.confidence());
        entity.setFraudHintsJson(request.fraudHintsJson());
        entity.setInspectionId(request.inspectionId());
        entity.setNotes(request.notes());
        entity.setAssessorId(actorId);
        entity.setAssessorUserId(actorId);
        entity.setAccepted(request.accepted() == null || request.accepted());
        entity.setAssessedAt(Instant.now());
        entity.setDeleted(false);
        entity.setCreatedBy(actorId);
        entity.setStatus("ACTIVE");
        assessmentRepository.save(entity);

        claim.setAssessedAmount(request.recommendedAmount());
        claim.setFinancialSnapshotJson(buildFinancialSnapshot(claim, residual, request.recommendedAmount()));
        claim.setUpdatedBy(actorId);
        claimRepository.save(claim);

        ClaimAssessmentResponse response = toResponse(entity);
        auditHelper.record(AuditAction.CLAIM_ASSESSMENT_RECORDED, actorId, "claim_assessment", entity.getId(), null, response, null);
        return response;
    }

    private String buildFinancialSnapshot(ClaimEntity claim, BigDecimal residual, BigDecimal recommended) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("claimedAmount", claim.getClaimedAmount());
        snap.put("assessedAmount", recommended);
        snap.put("coverageResidual", residual);
        snap.put("currency", claim.getCurrency());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private ClaimAssessmentResponse toResponse(ClaimAssessmentEntity e) {
        return new ClaimAssessmentResponse(
            e.getId(), e.getClaimId(), e.getInspectionId(), e.getMethodsJson(), e.getFindingsJson(),
            e.getRecommendedAmount(), e.getAssessedAmount(), e.getCurrency(), e.getConfidence(),
            e.getFraudHintsJson(), e.getAssessorId(), e.isAccepted(), e.getNotes(), e.getAssessedAt()
        );
    }
}
