package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.FarmerSubjectScope;
import com.aegisterra.platform.application.insurance.PolicyDocumentService;
import com.aegisterra.platform.application.insurance.PolicyService;
import com.aegisterra.platform.infrastructure.persistence.insurance.InsurancePolicyRepository;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.BeneficiaryResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.PolicyDocumentResponse;
import com.aegisterra.platform.application.contracts.PolicyResponse;
import com.aegisterra.platform.application.contracts.PolicySubmitRequest;
import com.aegisterra.platform.application.contracts.PolicyTransitionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies")
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyDocumentService documentService;
    private final InsurancePolicyRepository policyRepository;
    private final FarmerSubjectScope farmerSubjectScope;

    public PolicyController(
        PolicyService policyService,
        PolicyDocumentService documentService,
        InsurancePolicyRepository policyRepository,
        FarmerSubjectScope farmerSubjectScope
    ) {
        this.policyService = policyService;
        this.documentService = documentService;
        this.policyRepository = policyRepository;
        this.farmerSubjectScope = farmerSubjectScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('policies:read')")
    @Operation(summary = "Search policies")
    public PageResponse<PolicyResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String policyNumber,
        @RequestParam(required = false) UUID farmerId,
        @RequestParam(required = false) UUID farmId,
        @RequestParam(required = false) UUID productId,
        @RequestParam(required = false) UUID seasonId,
        @RequestParam(required = false) UUID cropId,
        @RequestParam(required = false) UUID companyId,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID scopedFarmerId = farmerSubjectScope.forceFarmerId(actor, farmerId);
        return policyService.search(
            q, policyNumber, scopedFarmerId, farmId, productId, seasonId, cropId, companyId, status, pageable
        );
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('policies:read')")
    public ResponseEntity<String> export(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=policies.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(policyService.exportCsv(q, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('policies:read')")
    public PolicyResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        PolicyResponse policy = policyService.get(id);
        farmerSubjectScope.assertOwnsFarmer(actor, policy.farmerId());
        return policy;
    }

    @GetMapping("/{id}/beneficiaries")
    @PreAuthorize("hasAuthority('policies:read')")
    public List<BeneficiaryResponse> beneficiaries(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        PolicyResponse policy = policyService.get(id);
        farmerSubjectScope.assertOwnsFarmer(actor, policy.farmerId());
        return policyService.listBeneficiaries(id);
    }

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('policies:read')")
    public List<PolicyDocumentResponse> documents(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        PolicyResponse policy = policyService.get(id);
        farmerSubjectScope.assertOwnsFarmer(actor, policy.farmerId());
        return documentService.list(id);
    }

    @PostMapping("/{id}/documents/regenerate")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyDocumentResponse regenerate(
        @PathVariable UUID id,
        @RequestParam String documentType,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        var policy = policyRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
        return documentService.regenerate(policy, documentType, actor.id());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('policies:write')")
    @Operation(summary = "Submit a new policy application")
    public ResponseEntity<PolicyResponse> submit(
        @Valid @RequestBody PolicySubmitRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.submit(request, actor.id()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('policies:approve','policies:write')")
    public PolicyResponse approve(
        @PathVariable UUID id,
        @RequestBody(required = false) PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.approve(id, actor.id(), request == null ? null : request.reason());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('policies:approve','policies:write')")
    public PolicyResponse reject(
        @PathVariable UUID id,
        @Valid @RequestBody PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.reject(id, actor.id(), request.reason());
    }

    @PostMapping("/{id}/mark-premium-paid")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse markPaid(
        @PathVariable UUID id,
        @RequestBody(required = false) PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.markPremiumPaid(id, actor.id(), request == null ? null : request.reason());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse activate(
        @PathVariable UUID id,
        @RequestBody(required = false) PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.activate(id, actor.id(), request == null ? "manual-activate" : request.reason());
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse suspend(
        @PathVariable UUID id,
        @Valid @RequestBody PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.suspend(id, actor.id(), request.reason());
    }

    @PostMapping("/{id}/reinstate")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse reinstate(
        @PathVariable UUID id,
        @RequestBody(required = false) PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.reinstate(id, actor.id(), request == null ? "reinstate" : request.reason());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse cancel(
        @PathVariable UUID id,
        @Valid @RequestBody PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.cancel(id, actor.id(), request.reason());
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse renew(
        @PathVariable UUID id,
        @RequestBody(required = false) PolicyTransitionRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return policyService.renew(id, actor.id(), request == null ? "renewal" : request.reason());
    }
}
