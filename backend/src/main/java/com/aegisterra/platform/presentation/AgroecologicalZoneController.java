package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.AgroecologicalZoneResponse;
import com.aegisterra.platform.application.geography.GeographyCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agroecological-zones")
@Tag(name = "Agroecological Zones")
public class AgroecologicalZoneController {

    private final GeographyCatalogService geographyCatalogService;

    public AgroecologicalZoneController(GeographyCatalogService geographyCatalogService) {
        this.geographyCatalogService = geographyCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "List agroecological zones")
    public List<AgroecologicalZoneResponse> list() {
        return geographyCatalogService.listZones();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Get agroecological zone by id")
    public AgroecologicalZoneResponse get(@PathVariable UUID id) {
        return geographyCatalogService.getZone(id);
    }
}
