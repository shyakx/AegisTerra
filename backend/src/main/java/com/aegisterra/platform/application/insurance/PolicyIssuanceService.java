package com.aegisterra.platform.application.insurance;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyIssuanceDraftEntity;
import com.aegisterra.platform.infrastructure.persistence.insurance.PolicyIssuanceDraftRepository;
import com.aegisterra.platform.application.contracts.PolicyIssuanceDraftRequest;
import com.aegisterra.platform.application.contracts.PolicyIssuanceDraftResponse;
import com.aegisterra.platform.application.contracts.PolicyResponse;
import com.aegisterra.platform.application.contracts.PolicySubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PolicyIssuanceService {

    private final PolicyIssuanceDraftRepository draftRepository;
    private final PolicyService policyService;
    private final InsuranceAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public PolicyIssuanceService(
        PolicyIssuanceDraftRepository draftRepository,
        PolicyService policyService,
        InsuranceAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.draftRepository = draftRepository;
        this.policyService = policyService;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PolicyIssuanceDraftResponse> listMine(UUID userId) {
        return draftRepository.findByCreatedByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PolicyIssuanceDraftResponse get(UUID id, UUID userId) {
        return toResponse(requireOwned(id, userId));
    }

    @Transactional
    public PolicyIssuanceDraftResponse create(PolicyIssuanceDraftRequest request, UUID userId) {
        PolicyIssuanceDraftEntity draft = new PolicyIssuanceDraftEntity();
        draft.setCreatedByUserId(userId);
        draft.setCreatedBy(userId);
        draft.setCurrentStep(request.currentStep());
        draft.setPayloadJson(request.payloadJson());
        draft.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        draft.setStatus("IN_PROGRESS");
        draft.setDeleted(false);
        draftRepository.save(draft);
        auditHelper.record(AuditAction.POLICY_DRAFT_SAVED, userId, "policy_issuance_draft", draft.getId(), null, toResponse(draft), null);
        return toResponse(draft);
    }

    @Transactional
    public PolicyIssuanceDraftResponse update(UUID id, PolicyIssuanceDraftRequest request, UUID userId) {
        PolicyIssuanceDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft is not editable");
        }
        PolicyIssuanceDraftResponse old = toResponse(draft);
        draft.setCurrentStep(request.currentStep());
        draft.setPayloadJson(request.payloadJson());
        draft.setUpdatedBy(userId);
        auditHelper.record(AuditAction.POLICY_DRAFT_SAVED, userId, "policy_issuance_draft", id, old, toResponse(draft), null);
        return toResponse(draft);
    }

    @Transactional
    public PolicyResponse submit(UUID id, UUID userId) {
        PolicyIssuanceDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft cannot be submitted");
        }
        try {
            PolicySubmitRequest request = objectMapper.readValue(draft.getPayloadJson(), PolicySubmitRequest.class);
            PolicyResponse policy = policyService.submit(request, userId);
            draft.setPolicyId(policy.id());
            draft.setStatus("SUBMITTED");
            draft.setUpdatedBy(userId);
            return policy;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid draft payload: " + ex.getMessage());
        }
    }

    private PolicyIssuanceDraftEntity requireOwned(UUID id, UUID userId) {
        PolicyIssuanceDraftEntity draft = draftRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!draft.getCreatedByUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Draft not owned by current user");
        }
        return draft;
    }

    private PolicyIssuanceDraftResponse toResponse(PolicyIssuanceDraftEntity draft) {
        return new PolicyIssuanceDraftResponse(
            draft.getId(), draft.getCreatedByUserId(), draft.getPolicyId(), draft.getCurrentStep(),
            draft.getPayloadJson(), draft.getStatus(), draft.getExpiresAt(), draft.getUpdatedAt()
        );
    }
}
