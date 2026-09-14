package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.insurance.PremiumPricingEngine;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.PremiumQuoteRequest;
import com.aegisterra.platform.application.contracts.PremiumQuoteResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Premiums")
public class PremiumController {

    private final PremiumPricingEngine pricingEngine;

    public PremiumController(PremiumPricingEngine pricingEngine) {
        this.pricingEngine = pricingEngine;
    }

    @PostMapping("/premiums/quote")
    @PreAuthorize("hasAuthority('policies:write')")
    public ResponseEntity<PremiumQuoteResponse> quote(
        @Valid @RequestBody PremiumQuoteRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingEngine.quote(request, actor.id()));
    }

    @GetMapping("/premium-quotes/{id}")
    @PreAuthorize("hasAuthority('policies:read')")
    public PremiumQuoteResponse get(@PathVariable UUID id) {
        return pricingEngine.get(id);
    }
}
