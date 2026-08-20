package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.insurance.PolicyIssuanceService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.PolicyIssuanceDraftRequest;
import com.aegisterra.platform.application.contracts.PolicyIssuanceDraftResponse;
import com.aegisterra.platform.application.contracts.PolicyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policy-issuance-drafts")
@Tag(name = "Policy Issuance Drafts")
public class PolicyIssuanceDraftController {

    private final PolicyIssuanceService issuanceService;

    public PolicyIssuanceDraftController(PolicyIssuanceService issuanceService) {
        this.issuanceService = issuanceService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('policies:write')")
    public List<PolicyIssuanceDraftResponse> listMine(@AuthenticationPrincipal AegisUserPrincipal actor) {
        return issuanceService.listMine(actor.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyIssuanceDraftResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        return issuanceService.get(id, actor.id());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('policies:write')")
    public ResponseEntity<PolicyIssuanceDraftResponse> create(
        @Valid @RequestBody PolicyIssuanceDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issuanceService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyIssuanceDraftResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody PolicyIssuanceDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return issuanceService.update(id, request, actor.id());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('policies:write')")
    public PolicyResponse submit(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        return issuanceService.submit(id, actor.id());
    }
}
