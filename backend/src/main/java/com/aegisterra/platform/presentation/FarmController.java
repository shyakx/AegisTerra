package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.FarmService;
import com.aegisterra.platform.application.agriculture.FarmerSubjectScope;
import com.aegisterra.platform.application.contracts.FarmRequest;
import com.aegisterra.platform.application.contracts.FarmResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/farms")
@Tag(name = "Farms")
public class FarmController {

    private final FarmService farmService;
    private final FarmerSubjectScope farmerSubjectScope;

    public FarmController(FarmService farmService, FarmerSubjectScope farmerSubjectScope) {
        this.farmService = farmService;
        this.farmerSubjectScope = farmerSubjectScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    @Operation(summary = "Search farms")
    public PageResponse<FarmResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) UUID farmerId,
        @RequestParam(required = false) UUID districtId,
        @RequestParam(required = false) UUID villageId,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID scopedFarmerId = farmerSubjectScope.forceFarmerId(actor, farmerId);
        return farmService.search(q, scopedFarmerId, districtId, villageId, status, pageable);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('farms:read')")
    public ResponseEntity<String> export(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) UUID farmerId,
        @RequestParam(required = false) UUID districtId,
        @RequestParam(required = false) UUID villageId,
        @RequestParam(required = false) String status,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        UUID scopedFarmerId = farmerSubjectScope.forceFarmerId(actor, farmerId);
        String csv = farmService.exportCsv(q, scopedFarmerId, districtId, villageId, status);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farms.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public FarmResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        FarmResponse farm = farmService.get(id);
        farmerSubjectScope.assertOwnsFarmer(actor, farm.farmerId());
        return farm;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<FarmResponse> create(
        @Valid @RequestBody FarmRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Farmers cannot create farms via operator API");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(farmService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public FarmResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody FarmRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        FarmResponse existing = farmService.get(id);
        farmerSubjectScope.assertOwnsFarmer(actor, existing.farmerId());
        return farmService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Farmers cannot delete farms via operator API");
        }
        farmService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
