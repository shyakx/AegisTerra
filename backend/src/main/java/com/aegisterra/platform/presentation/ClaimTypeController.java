package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.claims.ClaimService;
import com.aegisterra.platform.application.contracts.ClaimTypeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claim-types")
@Tag(name = "Claim Types")
public class ClaimTypeController {

    private final ClaimService claimService;

    public ClaimTypeController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('claims:read')")
    public List<ClaimTypeResponse> list() {
        return claimService.listTypes();
    }
}
