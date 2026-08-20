package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.CropSeasonService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.CropSeasonRequest;
import com.aegisterra.platform.application.contracts.CropSeasonResponse;
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
@RequestMapping("/api/v1/crop-seasons")
@Tag(name = "Crop Seasons")
public class CropSeasonController {

    private final CropSeasonService cropSeasonService;

    public CropSeasonController(CropSeasonService cropSeasonService) {
        this.cropSeasonService = cropSeasonService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    public List<CropSeasonResponse> list(@RequestParam UUID farmId) {
        return cropSeasonService.listByFarm(farmId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public CropSeasonResponse get(@PathVariable UUID id) {
        return cropSeasonService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<CropSeasonResponse> create(
        @Valid @RequestBody CropSeasonRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropSeasonService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public CropSeasonResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody CropSeasonRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return cropSeasonService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        cropSeasonService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
