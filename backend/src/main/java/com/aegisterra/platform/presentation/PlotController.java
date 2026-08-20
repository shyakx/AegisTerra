package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.PlotService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.PlotRequest;
import com.aegisterra.platform.application.contracts.PlotResponse;
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
@RequestMapping("/api/v1/plots")
@Tag(name = "Plots")
public class PlotController {

    private final PlotService plotService;

    public PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    public List<PlotResponse> list(@RequestParam UUID farmId) {
        return plotService.listByFarm(farmId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public PlotResponse get(@PathVariable UUID id) {
        return plotService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<PlotResponse> create(
        @Valid @RequestBody PlotRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plotService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public PlotResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody PlotRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return plotService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        plotService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
