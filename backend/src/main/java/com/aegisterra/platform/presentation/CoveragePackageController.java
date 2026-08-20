package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.insurance.CoveragePackageService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.CoveragePackageRequest;
import com.aegisterra.platform.application.contracts.CoveragePackageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coverage-packages")
@Tag(name = "Coverage Packages")
public class CoveragePackageController {

    private final CoveragePackageService packageService;

    public CoveragePackageController(CoveragePackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('policies:read')")
    public List<CoveragePackageResponse> list(@RequestParam UUID productId) {
        return packageService.listByProduct(productId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('policies:read')")
    public CoveragePackageResponse get(@PathVariable UUID id) {
        return packageService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('policies:write')")
    public ResponseEntity<CoveragePackageResponse> create(
        @Valid @RequestBody CoveragePackageRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(packageService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('policies:write')")
    public CoveragePackageResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody CoveragePackageRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return packageService.update(id, request, actor.id());
    }
}
