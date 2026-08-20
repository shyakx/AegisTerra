package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.HouseholdService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.HouseholdRequest;
import com.aegisterra.platform.application.contracts.HouseholdResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/households")
@Tag(name = "Households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Search households")
    public PageResponse<HouseholdResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return householdService.search(q, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    public HouseholdResponse get(@PathVariable UUID id) {
        return householdService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farmers:write')")
    public ResponseEntity<HouseholdResponse> create(
        @Valid @RequestBody HouseholdRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public HouseholdResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody HouseholdRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return householdService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        householdService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
