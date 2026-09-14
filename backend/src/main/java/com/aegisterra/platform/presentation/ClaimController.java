package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.FarmerSubjectScope;
import com.aegisterra.platform.application.claims.ClaimAssessmentService;
import com.aegisterra.platform.application.claims.ClaimDraftService;
import com.aegisterra.platform.application.claims.ClaimEvidenceService;
import com.aegisterra.platform.application.claims.ClaimInspectionService;
import com.aegisterra.platform.application.claims.ClaimService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.ClaimAssessmentRequest;
import com.aegisterra.platform.application.contracts.ClaimAssessmentResponse;
import com.aegisterra.platform.application.contracts.ClaimCancelRequest;
import com.aegisterra.platform.application.contracts.ClaimDraftRequest;
import com.aegisterra.platform.application.contracts.ClaimDraftResponse;
import com.aegisterra.platform.application.contracts.ClaimEvidenceRequest;
import com.aegisterra.platform.application.contracts.ClaimEvidenceResponse;
import com.aegisterra.platform.application.contracts.ClaimInspectionRequest;
import com.aegisterra.platform.application.contracts.ClaimInspectionResponse;
import com.aegisterra.platform.application.contracts.ClaimResponse;
import com.aegisterra.platform.application.contracts.ClaimSubmitRequest;
import com.aegisterra.platform.application.contracts.ClaimTimelineEntryResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/claims")
@Tag(name = "Claims")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimDraftService draftService;
    private final ClaimEvidenceService evidenceService;
    private final ClaimAssessmentService assessmentService;
    private final ClaimInspectionService inspectionService;
    private final FarmerSubjectScope farmerSubjectScope;

    public ClaimController(
        ClaimService claimService,
        ClaimDraftService draftService,
        ClaimEvidenceService evidenceService,
        ClaimAssessmentService assessmentService,
        ClaimInspectionService inspectionService,
        FarmerSubjectScope farmerSubjectScope
    ) {
        this.claimService = claimService;
        this.draftService = draftService;
        this.evidenceService = evidenceService;
        this.assessmentService = assessmentService;
        this.inspectionService = inspectionService;
        this.farmerSubjectScope = farmerSubjectScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('claims:read')")
    @Operation(summary = "Search claims")
    public PageResponse<ClaimResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String claimTypeCode,
        @RequestParam(required = false) UUID policyId,
        @RequestParam(required = false) UUID farmerId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID scopedFarmerId = farmerSubjectScope.forceFarmerId(actor, farmerId);
        return claimService.search(q, status, claimTypeCode, policyId, scopedFarmerId, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('claims:read')")
    public ClaimResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        ClaimResponse claim = claimService.get(id);
        if (claim.farmerId() != null) {
            farmerSubjectScope.assertOwnsFarmer(actor, claim.farmerId());
        }
        return claim;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('claims:write')")
    @Operation(summary = "Create a draft claim intake")
    public ResponseEntity<ClaimResponse> create(
        @Valid @RequestBody ClaimSubmitRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        ClaimResponse created = claimService.toResponse(claimService.createDraftClaim(request, actor.id()));
        if (created.farmerId() != null) {
            farmerSubjectScope.assertOwnsFarmer(actor, created.farmerId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('claims:write')")
    @Operation(summary = "Submit a draft claim and start workflow")
    public ClaimResponse submit(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return claimService.submitById(id, actor.id());
    }

    @GetMapping("/drafts")
    @PreAuthorize("hasAuthority('claims:write')")
    public List<ClaimDraftResponse> listDrafts(@AuthenticationPrincipal AegisUserPrincipal actor) {
        return draftService.listMine(actor.id());
    }

    @GetMapping("/drafts/{id}")
    @PreAuthorize("hasAuthority('claims:write')")
    public ClaimDraftResponse getDraft(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        return draftService.get(id, actor.id());
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasAuthority('claims:write')")
    public ResponseEntity<ClaimDraftResponse> createDraft(
        @Valid @RequestBody ClaimDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.create(request, actor.id()));
    }

    @PutMapping("/drafts/{id}")
    @PreAuthorize("hasAuthority('claims:write')")
    public ClaimDraftResponse updateDraft(
        @PathVariable UUID id,
        @Valid @RequestBody ClaimDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return draftService.update(id, request, actor.id());
    }

    @PostMapping("/drafts/{id}/submit")
    @PreAuthorize("hasAuthority('claims:write')")
    public ClaimResponse submitDraft(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        return draftService.submit(id, actor.id());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('claims:write')")
    public ClaimResponse cancel(
        @PathVariable UUID id,
        @Valid @RequestBody ClaimCancelRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return claimService.cancel(id, actor.id(), request.reason());
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('claims:read')")
    public List<ClaimTimelineEntryResponse> timeline(@PathVariable UUID id) {
        return claimService.timeline(id);
    }

    @GetMapping("/{id}/evidence")
    @PreAuthorize("hasAuthority('claims:read')")
    public List<ClaimEvidenceResponse> listEvidence(@PathVariable UUID id) {
        return evidenceService.list(id);
    }

    @PostMapping("/{id}/evidence")
    @PreAuthorize("hasAuthority('claims:write')")
    public ResponseEntity<ClaimEvidenceResponse> addEvidence(
        @PathVariable UUID id,
        @Valid @RequestBody ClaimEvidenceRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceService.add(id, request, actor.id()));
    }

    @DeleteMapping("/{id}/evidence/{evidenceId}")
    @PreAuthorize("hasAuthority('claims:write')")
    public ResponseEntity<Void> deleteEvidence(
        @PathVariable UUID id,
        @PathVariable UUID evidenceId,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        evidenceService.softDelete(id, evidenceId, actor.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assessments")
    @PreAuthorize("hasAuthority('claims:read')")
    public List<ClaimAssessmentResponse> listAssessments(@PathVariable UUID id) {
        return assessmentService.list(id);
    }

    @PostMapping("/{id}/assessments")
    @PreAuthorize("hasAuthority('claims:assess')")
    public ResponseEntity<ClaimAssessmentResponse> createAssessment(
        @PathVariable UUID id,
        @Valid @RequestBody ClaimAssessmentRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.create(id, request, actor.id()));
    }

    @GetMapping("/{id}/inspections")
    @PreAuthorize("hasAuthority('claims:read')")
    public List<ClaimInspectionResponse> listInspections(@PathVariable UUID id) {
        return inspectionService.list(id);
    }

    @PostMapping("/{id}/inspections")
    @PreAuthorize("hasAuthority('claims:assess')")
    public ResponseEntity<ClaimInspectionResponse> createInspection(
        @PathVariable UUID id,
        @RequestBody ClaimInspectionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inspectionService.scheduleOrComplete(id, request, actor.id()));
    }

    @PostMapping("/{id}/inspections/{inspectionId}/complete")
    @PreAuthorize("hasAuthority('claims:assess')")
    public ClaimInspectionResponse completeInspection(
        @PathVariable UUID id,
        @PathVariable UUID inspectionId,
        @RequestBody(required = false) ClaimInspectionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        ClaimInspectionRequest body = request == null
            ? new ClaimInspectionRequest(null, null, null, null, null, null, null, null, true)
            : request;
        return inspectionService.complete(id, inspectionId, body, actor.id());
    }
}
