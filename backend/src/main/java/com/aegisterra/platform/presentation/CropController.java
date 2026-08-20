package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.CropService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.CropRequest;
import com.aegisterra.platform.application.contracts.CropResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crops")
@Tag(name = "Crops")
public class CropController {

    private final CropService cropService;

    public CropController(CropService cropService) {
        this.cropService = cropService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    public List<CropResponse> list() {
        return cropService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public CropResponse get(@PathVariable UUID id) {
        return cropService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<CropResponse> create(
        @Valid @RequestBody CropRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cropService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public CropResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody CropRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return cropService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        cropService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
