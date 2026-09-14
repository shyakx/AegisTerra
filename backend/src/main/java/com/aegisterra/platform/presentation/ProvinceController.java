package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.contracts.ProvinceResponse;
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
@RequestMapping("/api/v1/provinces")
@Tag(name = "Provinces")
public class ProvinceController {

    private final GeographyCatalogService geographyCatalogService;

    public ProvinceController(GeographyCatalogService geographyCatalogService) {
        this.geographyCatalogService = geographyCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "List provinces")
    public List<ProvinceResponse> list() {
        return geographyCatalogService.listProvinces();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farmers:read')")
    @Operation(summary = "Get province by id")
    public ProvinceResponse get(@PathVariable UUID id) {
        return geographyCatalogService.getProvince(id);
    }
}
