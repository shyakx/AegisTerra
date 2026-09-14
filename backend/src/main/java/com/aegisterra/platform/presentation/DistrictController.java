package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.DistrictResponse;
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
@RequestMapping("/api/v1/districts")
@Tag(name = "Districts")
public class DistrictController {

    private final GeographyCatalogService geographyCatalogService;

    public DistrictController(GeographyCatalogService geographyCatalogService) {
        this.geographyCatalogService = geographyCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "List districts")
    public List<DistrictResponse> list(@RequestParam(required = false) UUID provinceId) {
        return geographyCatalogService.listDistricts(provinceId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Get district by id")
    public DistrictResponse get(@PathVariable UUID id) {
        return geographyCatalogService.getDistrict(id);
    }
}
