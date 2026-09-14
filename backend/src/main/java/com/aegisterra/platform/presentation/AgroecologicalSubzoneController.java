package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.AgroecologicalSubzoneResponse;
import com.aegisterra.platform.application.geography.GeographyCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agroecological-subzones")
@Tag(name = "Agroecological Sub-zones")
public class AgroecologicalSubzoneController {

    private final GeographyCatalogService geographyCatalogService;

    public AgroecologicalSubzoneController(GeographyCatalogService geographyCatalogService) {
        this.geographyCatalogService = geographyCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "List agroecological sub-zones")
    public List<AgroecologicalSubzoneResponse> list(
        @RequestParam(required = false) UUID zoneId,
        @RequestParam(required = false) UUID districtId
    ) {
        return geographyCatalogService.listSubzones(zoneId, districtId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Get agroecological sub-zone by id")
    public AgroecologicalSubzoneResponse get(@PathVariable UUID id) {
        return geographyCatalogService.getSubzone(id);
    }
}
