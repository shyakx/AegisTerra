package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.application.claims.spi.FraudScoringPort;
import com.aegisterra.platform.application.events.EventBus;
import com.aegisterra.platform.application.workflow.WorkflowInstanceService;
import com.aegisterra.platform.domain.claims.ClaimStatus;
import com.aegisterra.platform.domain.events.DomainEventTypes;
import com.aegisterra.platform.domain.events.PlatformDomainEvent;
import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.domain.insurance.PolicyStatus;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimEvidenceEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimEvidenceRepository;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimStatusHistoryRepository;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimTypeDocumentRuleEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimTypeDocumentRuleRepository;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimTypeEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimTypeRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionEntity;
import com.aegisterra.platform.infrastructure.persistence.workflow.WorkflowDefinitionRepository;
import com.aegisterra.platform.application.contracts.ClaimResponse;
import com.aegisterra.platform.application.contracts.ClaimSubmitRequest;
import com.aegisterra.platform.application.contracts.ClaimTimelineEntryResponse;
import com.aegisterra.platform.application.contracts.ClaimTypeResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.WorkflowInstanceResponse;
import com.aegisterra.platform.application.contracts.WorkflowStartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final InsurancePolicyRepository policyRepository;
    private final ClaimTypeRepository claimTypeRepository;
    private final ClaimTypeDocumentRuleRepository documentRuleRepository;
    private final ClaimEvidenceRepository evidenceRepository;
    private final ClaimStatusHistoryRepository historyRepository;
    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowInstanceService workflowInstanceService;
    private final ClaimLifecycleService lifecycleService;
    private final CoverageResidualService residualService;
    private final ClaimNumberGenerator numberGenerator;
    private final FraudScoringPort fraudScoringPort;
    private final ClaimsAuditHelper auditHelper;
    private final EventBus eventBus;
    private final ObjectMapper objectMapper;

    public ClaimService(
        ClaimRepository claimRepository,
        InsurancePolicyRepository policyRepository,
        ClaimTypeRepository claimTypeRepository,
        ClaimTypeDocumentRuleRepository documentRuleRepository,
        ClaimEvidenceRepository evidenceRepository,
        ClaimStatusHistoryRepository historyRepository,
        WorkflowDefinitionRepository definitionRepository,
        WorkflowInstanceService workflowInstanceService,
        ClaimLifecycleService lifecycleService,
        CoverageResidualService residualService,
        ClaimNumberGenerator numberGenerator,
        FraudScoringPort fraudScoringPort,
        ClaimsAuditHelper auditHelper,
        EventBus eventBus,
        ObjectMapper objectMapper
    ) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.claimTypeRepository = claimTypeRepository;
        this.documentRuleRepository = documentRuleRepository;
        this.evidenceRepository = evidenceRepository;
        this.historyRepository = historyRepository;
        this.definitionRepository = definitionRepository;
        this.workflowInstanceService = workflowInstanceService;
        this.lifecycleService = lifecycleService;
        this.residualService = residualService;
        this.numberGenerator = numberGenerator;
        this.fraudScoringPort = fraudScoringPort;
        this.auditHelper = auditHelper;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> search(
        String q, String status, String claimTypeCode, UUID policyId, UUID farmerId,
        LocalDate fromDate, LocalDate toDate, Pageable pageable
    ) {
        return PageResponse.from(claimRepository.search(
            blank(q), blank(status), blank(claimTypeCode), policyId, farmerId, fromDate, toDate, pageable
        ).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ClaimResponse get(UUID id) {
        return toResponse(lifecycleService.require(id));
    }

    @Transactional(readOnly = true)
    public List<ClaimTypeResponse> listTypes() {
        return claimTypeRepository.findByDeletedFalseOrderByCodeAsc().stream()
            .map(t -> new ClaimTypeResponse(
                t.getId(), t.getCode(), t.getName(), t.getDescription(), t.getNature(),
                t.getWorkflowDefinitionCode(), t.getAssessmentProfileJson(), t.isRequiresGeo(), t.getStatus()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimTimelineEntryResponse> timeline(UUID claimId) {
        lifecycleService.require(claimId);
        return historyRepository.findByClaimIdAndDeletedFalseOrderByOccurredAtAsc(claimId).stream()
            .map(h -> new ClaimTimelineEntryResponse(
                "STATUS", h.getFromStatus(), h.getToStatus(), h.getReason(), h.getActorId(), h.getOccurredAt()
            ))
            .toList();
    }

    @Transactional
    public ClaimResponse createAndSubmit(ClaimSubmitRequest request, UUID actorId) {
        ClaimEntity claim = createDraftClaim(request, actorId);
        return submitExisting(claim, actorId, request.correlationId());
    }

    @Transactional
    public ClaimResponse submitById(UUID claimId, UUID actorId) {
        ClaimEntity claim = lifecycleService.require(claimId);
        return submitExisting(claim, actorId, claim.getCorrelationId());
    }

    @Transactional
    public ClaimEntity createDraftClaim(ClaimSubmitRequest request, UUID actorId) {
        InsurancePolicyEntity policy = requireActivePolicy(request.policyId());
        ClaimTypeEntity claimType = requireClaimType(request.claimTypeCode());
        validateIncidentWindow(policy, request.incidentDate());
        String currency = request.currency() == null || request.currency().isBlank()
            ? policy.getCurrency() : request.currency().trim().toUpperCase();
        if (!currency.equalsIgnoreCase(policy.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Claim currency must match policy currency");
        }
        if (request.description() == null || request.description().trim().length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must be at least 5 characters");
        }

        ClaimEntity claim = new ClaimEntity();
        claim.setClaimNumber(numberGenerator.next());
        claim.setPolicyId(policy.getId());
        claim.setClaimTypeCode(claimType.getCode());
        claim.setFarmerId(policy.getFarmerId());
        claim.setFarmId(request.farmId() != null ? request.farmId() : policy.getFarmId());
        claim.setSeasonId(request.seasonId() != null ? request.seasonId() : policy.getSeasonId());
        claim.setCropId(request.cropId() != null ? request.cropId() : policy.getCropId());
        claim.setCauseOfLoss(request.causeOfLoss());
        claim.setDescription(request.description().trim());
        claim.setClaimedAmount(request.claimedAmount());
        claim.setCurrency(currency);
        claim.setIncidentDate(request.incidentDate());
        claim.setCorrelationId(request.correlationId() == null ? UUID.randomUUID().toString() : request.correlationId());
        claim.setStatus(ClaimStatus.DRAFT.name());
        claim.setDeleted(false);
        claim.setCreatedBy(actorId);
        claimRepository.save(claim);
        return claim;
    }

    @Transactional
    public ClaimResponse submitExisting(ClaimEntity claim, UUID actorId, String correlationId) {
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())
            && !ClaimStatus.RETURNED_FOR_INFO.name().equals(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT or RETURNED_FOR_INFO claims can be submitted");
        }
        InsurancePolicyEntity policy = requireActivePolicy(claim.getPolicyId());
        ClaimTypeEntity claimType = requireClaimType(claim.getClaimTypeCode());
        validateIncidentWindow(policy, claim.getIncidentDate());
        if (!claim.getCurrency().equalsIgnoreCase(policy.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Claim currency must match policy currency");
        }
        validateEvidenceMinimum(claim.getId(), claimType.getCode());

        BigDecimal residual = residualService.residualForPolicy(policy.getId(), claim.getId());
        if (claim.getClaimedAmount().compareTo(residual) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Claimed amount exceeds coverage residual (" + residual + ")");
        }

        FraudScoringPort.FraudScore fraud = fraudScoringPort.score(claim.getId(), policy.getId(), claim.getFarmerId());
        claim.setFraudTier(fraud.tier());
        claim.setFraudScore(fraud.score());
        try {
            claim.setFraudFlagsJson(objectMapper.writeValueAsString(fraud.flags()));
        } catch (Exception ex) {
            claim.setFraudFlagsJson("[]");
        }

        claim.setCoverageSnapshotJson(buildCoverageSnapshot(policy, residual));
        claim.setFinancialSnapshotJson(buildFinancialSnapshot(claim, residual));
        if (correlationId != null && !correlationId.isBlank()) {
            claim.setCorrelationId(correlationId);
        }
        claim.setSubmittedAt(Instant.now());
        claim.setUpdatedBy(actorId);

        if (ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            lifecycleService.transition(claim, ClaimStatus.SUBMITTED, actorId, "submit");
        } else {
            lifecycleService.transition(claim, ClaimStatus.SUBMITTED, actorId, "resubmit");
        }
        lifecycleService.transition(claim, ClaimStatus.UNDER_VALIDATION, actorId, "workflow-start");

        WorkflowDefinitionEntity definition = definitionRepository
            .findByCodeAndDeletedFalse(claimType.getWorkflowDefinitionCode())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Workflow definition not found: " + claimType.getWorkflowDefinitionCode()));

        WorkflowInstanceResponse instance = workflowInstanceService.start(
            new WorkflowStartRequest(
                definition.getId(),
                "CLAIM",
                claim.getId(),
                claim.getCorrelationId(),
                "{\"claimNumber\":\"" + claim.getClaimNumber() + "\"}"
            ),
            actorId
        );
        claim.setWorkflowInstanceId(instance.id());
        claim.setWorkflowDefinitionCode(claimType.getWorkflowDefinitionCode());
        claimRepository.save(claim);

        auditHelper.record(AuditAction.CLAIM_SUBMITTED, actorId, "claim", claim.getId(), null, toResponse(claim), null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("claimNumber", claim.getClaimNumber());
        payload.put("policyId", claim.getPolicyId().toString());
        payload.put("status", claim.getStatus());
        payload.put("workflowInstanceId", instance.id().toString());
        eventBus.publish(PlatformDomainEvent.of(
            DomainEventTypes.CLAIM_SUBMITTED,
            actorId,
            "CLAIM",
            claim.getId(),
            claim.getCorrelationId(),
            payload
        ));
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse cancel(UUID id, UUID actorId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason is required");
        }
        ClaimEntity claim = lifecycleService.require(id);
        ClaimStatus status = ClaimStatus.parse(claim.getStatus());
        if (status == ClaimStatus.APPROVED || status == ClaimStatus.PAYMENT_PENDING
            || status == ClaimStatus.SETTLED || status == ClaimStatus.CLOSURE_PENDING
            || status == ClaimStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Claim cannot be cancelled in status " + status);
        }
        lifecycleService.transition(claim, ClaimStatus.CANCELLED, actorId, reason);
        auditHelper.record(AuditAction.CLAIM_CANCELLED, actorId, "claim", id, null, toResponse(claim), reason);
        return toResponse(claim);
    }

    public ClaimResponse toResponse(ClaimEntity c) {
        return new ClaimResponse(
            c.getId(), c.getClaimNumber(), c.getPolicyId(), c.getClaimTypeCode(), c.getFarmerId(),
            c.getFarmId(), c.getSeasonId(), c.getCropId(), c.getCauseOfLoss(), c.getDescription(),
            c.getClaimedAmount(), c.getAssessedAmount(), c.getApprovedAmount(), c.getCurrency(),
            c.getIncidentDate(), c.getStatus(), c.getCoverageSnapshotJson(), c.getFinancialSnapshotJson(),
            c.getWorkflowInstanceId(), c.getWorkflowDefinitionCode(), c.getFraudTier(), c.getFraudScore(),
            c.getSubmittedAt(), c.getClosedAt(), c.getReasonCode(), c.getCorrelationId()
        );
    }

    private InsurancePolicyEntity requireActivePolicy(UUID policyId) {
        InsurancePolicyEntity policy = policyRepository.findByIdAndDeletedFalse(policyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
        if (!PolicyStatus.ACTIVE.name().equals(policy.getStatus())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Policy must be ACTIVE to file a claim");
        }
        return policy;
    }

    private ClaimTypeEntity requireClaimType(String code) {
        return claimTypeRepository.findByCodeAndDeletedFalse(code.trim().toUpperCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown claim type: " + code));
    }

    private void validateIncidentWindow(InsurancePolicyEntity policy, LocalDate incidentDate) {
        if (incidentDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incidentDate is required");
        }
        if (incidentDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incidentDate cannot be in the future");
        }
        if (incidentDate.isBefore(policy.getStartDate()) || incidentDate.isAfter(policy.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "incidentDate must fall within policy coverage window");
        }
    }

    private void validateEvidenceMinimum(UUID claimId, String claimTypeCode) {
        List<ClaimTypeDocumentRuleEntity> rules = documentRuleRepository.findByClaimTypeCodeAndDeletedFalse(claimTypeCode);
        Map<String, Long> counts = evidenceRepository.findByClaimIdAndDeletedFalseOrderByCreatedAtDesc(claimId).stream()
            .collect(Collectors.groupingBy(e -> e.getDocumentType().toUpperCase(), Collectors.counting()));
        for (ClaimTypeDocumentRuleEntity rule : rules) {
            if (!rule.isRequired()) {
                continue;
            }
            long have = counts.getOrDefault(rule.getDocumentType().toUpperCase(), 0L);
            if (have < rule.getMinCount()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Missing required evidence: " + rule.getDocumentType() + " (need " + rule.getMinCount() + ")");
            }
        }
    }

    private String buildCoverageSnapshot(InsurancePolicyEntity policy, BigDecimal residual) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("policyId", policy.getId().toString());
        snap.put("policyNumber", policy.getPolicyNumber());
        snap.put("coverageAmount", policy.getCoverageAmount());
        snap.put("residualCoverage", residual);
        snap.put("currency", policy.getCurrency());
        snap.put("startDate", policy.getStartDate().toString());
        snap.put("endDate", policy.getEndDate().toString());
        snap.put("productId", policy.getProductId() == null ? null : policy.getProductId().toString());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String buildFinancialSnapshot(ClaimEntity claim, BigDecimal residual) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("claimedAmount", claim.getClaimedAmount());
        snap.put("assessedAmount", claim.getAssessedAmount());
        snap.put("approvedAmount", claim.getApprovedAmount());
        snap.put("coverageResidual", residual);
        snap.put("currency", claim.getCurrency());
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
