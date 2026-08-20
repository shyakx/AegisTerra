package com.aegisterra.platform.application.claims;

import com.aegisterra.platform.domain.identity.AuditAction;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimDraftEntity;
import com.aegisterra.platform.infrastructure.persistence.claims.ClaimDraftRepository;
import com.aegisterra.platform.infrastructure.persistence.insurance.ClaimEntity;
import com.aegisterra.platform.application.contracts.ClaimDraftRequest;
import com.aegisterra.platform.application.contracts.ClaimDraftResponse;
import com.aegisterra.platform.application.contracts.ClaimResponse;
import com.aegisterra.platform.application.contracts.ClaimSubmitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClaimDraftService {

    private final ClaimDraftRepository draftRepository;
    private final ClaimService claimService;
    private final ClaimsAuditHelper auditHelper;
    private final ObjectMapper objectMapper;

    public ClaimDraftService(
        ClaimDraftRepository draftRepository,
        ClaimService claimService,
        ClaimsAuditHelper auditHelper,
        ObjectMapper objectMapper
    ) {
        this.draftRepository = draftRepository;
        this.claimService = claimService;
        this.auditHelper = auditHelper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ClaimDraftResponse> listMine(UUID userId) {
        return draftRepository.findByOwnerUserIdAndDeletedFalse(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClaimDraftResponse get(UUID id, UUID userId) {
        return toResponse(requireOwned(id, userId));
    }

    @Transactional
    public ClaimDraftResponse create(ClaimDraftRequest request, UUID userId) {
        ClaimSubmitRequest submitRequest = parsePayload(request.payloadJson());
        ClaimEntity claim = claimService.createDraftClaim(submitRequest, userId);

        String enrichedPayload = enrichPayload(request.payloadJson(), claim.getId());
        ClaimDraftEntity draft = new ClaimDraftEntity();
        draft.setOwnerUserId(userId);
        draft.setCreatedBy(userId);
        draft.setCurrentStep(request.currentStep());
        draft.setPayloadJson(enrichedPayload);
        draft.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        draft.setStatus("IN_PROGRESS");
        draft.setDeleted(false);
        draftRepository.save(draft);

        ClaimDraftResponse response = toResponse(draft);
        auditHelper.record(AuditAction.CLAIM_DRAFT_SAVED, userId, "claim_draft", draft.getId(), null, response, null);
        return response;
    }

    @Transactional
    public ClaimDraftResponse update(UUID id, ClaimDraftRequest request, UUID userId) {
        ClaimDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft is not editable");
        }
        ClaimDraftResponse old = toResponse(draft);
        UUID claimId = extractClaimId(draft.getPayloadJson());
        String payload = request.payloadJson();
        if (claimId != null) {
            payload = enrichPayload(payload, claimId);
        }
        draft.setCurrentStep(request.currentStep());
        draft.setPayloadJson(payload);
        draft.setUpdatedBy(userId);
        ClaimDraftResponse response = toResponse(draft);
        auditHelper.record(AuditAction.CLAIM_DRAFT_SAVED, userId, "claim_draft", id, old, response, null);
        return response;
    }

    @Transactional
    public ClaimResponse submit(UUID id, UUID userId) {
        ClaimDraftEntity draft = requireOwned(id, userId);
        if (!"IN_PROGRESS".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft cannot be submitted");
        }
        UUID claimId = extractClaimId(draft.getPayloadJson());
        if (claimId == null) {
            ClaimSubmitRequest request = parsePayload(draft.getPayloadJson());
            ClaimEntity created = claimService.createDraftClaim(request, userId);
            claimId = created.getId();
            draft.setPayloadJson(enrichPayload(draft.getPayloadJson(), claimId));
        }
        ClaimResponse response = claimService.submitById(claimId, userId);
        draft.setStatus("SUBMITTED");
        draft.setUpdatedBy(userId);
        return response;
    }

    private ClaimDraftEntity requireOwned(UUID id, UUID userId) {
        ClaimDraftEntity draft = draftRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!draft.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Draft not owned by current user");
        }
        return draft;
    }

    private ClaimSubmitRequest parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ClaimSubmitRequest.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid draft payload: " + ex.getMessage());
        }
    }

    private String enrichPayload(String payloadJson, UUID claimId) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            ObjectNode object = node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
            object.put("claimId", claimId.toString());
            return objectMapper.writeValueAsString(object);
        } catch (Exception ex) {
            return payloadJson;
        }
    }

    private UUID extractClaimId(String payloadJson) {
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            if (node.hasNonNull("claimId")) {
                return UUID.fromString(node.get("claimId").asText());
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private ClaimDraftResponse toResponse(ClaimDraftEntity draft) {
        return new ClaimDraftResponse(
            draft.getId(),
            draft.getOwnerUserId(),
            extractClaimId(draft.getPayloadJson()),
            draft.getCurrentStep(),
            draft.getPayloadJson(),
            draft.getStatus(),
            draft.getExpiresAt(),
            draft.getUpdatedAt()
        );
    }
}
