package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.FarmerService;
import com.aegisterra.platform.application.agriculture.FarmerSubjectScope;
import com.aegisterra.platform.application.contracts.FarmerRequest;
import com.aegisterra.platform.application.contracts.FarmerResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
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

@RestController
@RequestMapping("/api/v1/farmers")
@Tag(name = "Farmers")
public class FarmerController {

    private final FarmerService farmerService;
    private final FarmerSubjectScope farmerSubjectScope;

    public FarmerController(FarmerService farmerService, FarmerSubjectScope farmerSubjectScope) {
        this.farmerService = farmerService;
        this.farmerSubjectScope = farmerSubjectScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Search farmers")
    public PageResponse<FarmerResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String nationalId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String farmerCode,
        @RequestParam(required = false) UUID householdId,
        @RequestParam(required = false) UUID districtId,
        @RequestParam(required = false) UUID villageId,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        Optional<UUID> owned = farmerSubjectScope.subjectFarmerId(actor);
        if (owned.isPresent()) {
            FarmerResponse me = farmerService.get(owned.get());
            return PageResponse.from(new PageImpl<>(List.of(me), pageable, 1));
        }
        return farmerService.search(
            q, nationalId, phone, farmerCode, householdId, districtId, villageId, status, pageable
        );
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Export farmers CSV")
    public ResponseEntity<String> export(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String nationalId,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String farmerCode,
        @RequestParam(required = false) UUID householdId,
        @RequestParam(required = false) UUID districtId,
        @RequestParam(required = false) UUID villageId,
        @RequestParam(required = false) String status,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        Optional<UUID> owned = farmerSubjectScope.subjectFarmerId(actor);
        String csv;
        if (owned.isPresent()) {
            FarmerResponse me = farmerService.get(owned.get());
            csv = farmerService.exportCsv(null, me.nationalId(), null, me.farmerCode(), null, null, null, null);
        } else {
            csv = farmerService.exportCsv(q, nationalId, phone, farmerCode, householdId, districtId, villageId, status);
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farmers.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    public FarmerResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        farmerSubjectScope.assertOwnsFarmer(actor, id);
        return farmerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farmers:write')")
    public ResponseEntity<FarmerResponse> create(
        @Valid @RequestBody FarmerRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(farmerService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public FarmerResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody FarmerRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        farmerSubjectScope.assertOwnsFarmer(actor, id);
        return farmerService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        farmerService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
