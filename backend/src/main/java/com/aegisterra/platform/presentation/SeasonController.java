package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.agriculture.SeasonService;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.SeasonRequest;
import com.aegisterra.platform.application.contracts.SeasonResponse;
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
@RequestMapping("/api/v1/seasons")
@Tag(name = "Seasons")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farms:read')")
    public List<SeasonResponse> list() {
        return seasonService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:read')")
    public SeasonResponse get(@PathVariable UUID id) {
        return seasonService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<SeasonResponse> create(
        @Valid @RequestBody SeasonRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.create(request, actor.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public SeasonResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody SeasonRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return seasonService.update(id, request, actor.id());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('farms:write')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        seasonService.delete(id, actor.id());
        return ResponseEntity.noContent().build();
    }
}
