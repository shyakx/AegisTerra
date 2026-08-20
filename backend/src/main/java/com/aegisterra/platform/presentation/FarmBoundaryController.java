package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.BoundaryService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.FarmBoundaryRequest;
import com.aegisterra.platform.application.contracts.FarmBoundaryResponse;
import com.aegisterra.platform.application.contracts.GeometryValidateRequest;
import com.aegisterra.platform.application.contracts.GeometryValidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/v1/farm-boundaries")
@Tag(name = "Farm Boundaries")
public class FarmBoundaryController {

    private final BoundaryService boundaryService;

    public FarmBoundaryController(BoundaryService boundaryService) {
        this.boundaryService = boundaryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    public List<FarmBoundaryResponse> list(@RequestParam UUID farmId) {
        return boundaryService.listByFarm(farmId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public FarmBoundaryResponse get(@PathVariable UUID id) {
        return boundaryService.get(id);
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('farms:write')")
    @Operation(summary = "Validate GeoJSON geometry without saving")
    public GeometryValidateResponse validate(@Valid @RequestBody GeometryValidateRequest request) {
        return boundaryService.validate(request);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<FarmBoundaryResponse> create(
        @Valid @RequestBody FarmBoundaryRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boundaryService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public FarmBoundaryResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody FarmBoundaryRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return boundaryService.update(id, request, actor.id());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('farms:write')")
    public FarmBoundaryResponse activate(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor,
        @RequestParam(required = false) String reason
    ) {
        return boundaryService.activate(id, actor.id(), reason);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        boundaryService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
