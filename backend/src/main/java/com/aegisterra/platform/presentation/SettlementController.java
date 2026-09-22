package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.settlement.SettlementService;
import com.aegisterra.platform.application.agriculture.FarmerSubjectScope;
import com.aegisterra.platform.infrastructure.security.AegisUserPrincipal;
import com.aegisterra.platform.application.contracts.LedgerEntryResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import com.aegisterra.platform.application.contracts.SettlementCreateRequest;
import com.aegisterra.platform.application.contracts.SettlementManualConfirmRequest;
import com.aegisterra.platform.application.contracts.SettlementResponse;
import com.aegisterra.platform.application.contracts.SettlementTimelineEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/settlements")
@Tag(name = "Settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final FarmerSubjectScope farmerSubjectScope;

    public SettlementController(SettlementService settlementService, FarmerSubjectScope farmerSubjectScope) {
        this.settlementService = settlementService;
        this.farmerSubjectScope = farmerSubjectScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settlements:read')")
    @Operation(summary = "Search settlements")
    public PageResponse<SettlementResponse> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String sourceModule,
        @RequestParam(required = false) String providerCode,
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromCreated,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toCreated,
        @RequestParam(required = false) BigDecimal fromAmount,
        @RequestParam(required = false) BigDecimal toAmount,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        String providerFilter = providerCode != null ? providerCode : provider;
        Instant from = fromDate != null ? fromDate : fromCreated;
        Instant to = toDate != null ? toDate : toCreated;
        BigDecimal min = fromAmount != null ? fromAmount : minAmount;
        BigDecimal max = toAmount != null ? toAmount : maxAmount;
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return settlementService.searchForFarmer(q, status, sourceModule, providerFilter, from, to, min, max,
                farmerSubjectScope.requireSubjectFarmerId(actor), pageable);
        }
        return settlementService.search(q, status, sourceModule, providerFilter, from, to, min, max, pageable);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('settlements:read')")
    @Operation(summary = "Search settlements (alias)")
    public PageResponse<SettlementResponse> searchAlias(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String sourceModule,
        @RequestParam(required = false) String providerCode,
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
        @RequestParam(required = false) BigDecimal fromAmount,
        @RequestParam(required = false) BigDecimal toAmount,
        @PageableDefault(size = 20) Pageable pageable,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return search(q, status, sourceModule, providerCode, provider, fromDate, toDate, null, null,
            fromAmount, toAmount, null, null, pageable, actor);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('settlements:read')")
    @Operation(summary = "Search settlements (POST body)")
    public PageResponse<SettlementResponse> searchPost(
        @RequestBody(required = false) java.util.Map<String, Object> body,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        java.util.Map<String, Object> filters = body == null ? java.util.Map.of() : body;
        return settlementService.search(
            asString(filters.get("q")),
            asString(filters.get("status")),
            asString(filters.get("sourceModule")),
            asString(filters.get("providerCode") != null ? filters.get("providerCode") : filters.get("provider")),
            null, null, null, null, pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('settlements:read')")
    public SettlementResponse get(@PathVariable UUID id, @AuthenticationPrincipal AegisUserPrincipal actor) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return settlementService.getForFarmer(id, farmerSubjectScope.requireSubjectFarmerId(actor));
        }
        return settlementService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settlements:write')")
    @Operation(summary = "Create a manual settlement")
    public ResponseEntity<SettlementResponse> create(
        @Valid @RequestBody SettlementCreateRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(settlementService.create(request, actor.id()));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAuthority('settlements:read')")
    public List<SettlementTimelineEntryResponse> timeline(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return settlementService.timelineForFarmer(id, farmerSubjectScope.requireSubjectFarmerId(actor));
        }
        return settlementService.timeline(id);
    }

    @GetMapping("/{id}/ledger")
    @PreAuthorize("hasAuthority('settlements:read') or hasAuthority('ledger:read')")
    public List<LedgerEntryResponse> ledger(
        @PathVariable UUID id,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        if (farmerSubjectScope.isSubjectScoped(actor)) {
            return settlementService.ledgerForFarmer(id, farmerSubjectScope.requireSubjectFarmerId(actor));
        }
        return settlementService.ledger(id);
    }

    @PostMapping("/{id}/manual-confirm")
    @PreAuthorize("hasAuthority('settlements:process')")
    @Operation(summary = "Manually confirm a disbursed settlement")
    public SettlementResponse manualConfirm(
        @PathVariable UUID id,
        @Valid @RequestBody SettlementManualConfirmRequest request,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return settlementService.manualConfirm(id, request, actor.id());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('settlements:write')")
    public SettlementResponse cancel(
        @PathVariable UUID id,
        @RequestParam String reason,
        @AuthenticationPrincipal AegisUserPrincipal actor
    ) {
        return settlementService.cancel(id, actor.id(), reason);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
