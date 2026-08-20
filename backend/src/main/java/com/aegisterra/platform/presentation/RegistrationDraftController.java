package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.RegistrationService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.RegistrationDraftRequest;
import com.aegisterra.platform.application.contracts.RegistrationDraftResponse;
import com.aegisterra.platform.application.contracts.RegistrationSubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/registration-drafts")
@Tag(name = "Registration Drafts")
public class RegistrationDraftController {

    private final RegistrationService registrationService;

    public RegistrationDraftController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:write')")
    public List<RegistrationDraftResponse> listMine(@AuthenticationPrincipal AegisUserPrincipal actor) {
        return registrationService.listMine(actor.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public RegistrationDraftResponse get(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return registrationService.get(id, actor.id());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farmers:write')")
    public ResponseEntity<RegistrationDraftResponse> create(
        @Valid @RequestBody RegistrationDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public RegistrationDraftResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody RegistrationDraftRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return registrationService.update(id, request, actor.id());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('farmers:write')")
    @Operation(summary = "Submit registration draft and create agri aggregates")
    public RegistrationSubmitResponse submit(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return registrationService.submit(id, actor.id());
    }
}
