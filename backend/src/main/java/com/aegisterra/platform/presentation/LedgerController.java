package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.settlement.LedgerService;
import com.aegisterra.platform.application.contracts.LedgerEntryResponse;
import com.aegisterra.platform.application.contracts.LedgerTransactionResponse;
import com.aegisterra.platform.application.contracts.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import com.aegisterra.platform.infrastructure.config.ConditionalOnPartnerOps;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnPartnerOps
@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ledger:read')")
    @Operation(summary = "List ledger transactions (optionally filter by settlement)")
    public PageResponse<LedgerEntryResponse> list(
        @RequestParam(required = false) UUID settlementId,
        @PageableDefault(size = 20, sort = "postedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ledgerService.list(settlementId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ledger:read')")
    @Operation(summary = "Get ledger transaction or entry by id")
    public Object get(@PathVariable UUID id) {
        return ledgerService.getById(id);
    }

    @GetMapping("/settlements/{settlementId}")
    @PreAuthorize("hasAuthority('ledger:read')")
    public List<LedgerEntryResponse> bySettlement(@PathVariable UUID settlementId) {
        return ledgerService.entriesForSettlement(settlementId);
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("hasAuthority('ledger:read')")
    public LedgerTransactionResponse transaction(@PathVariable UUID id) {
        return ledgerService.getTransaction(id);
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('ledger:read')")
    public List<LedgerEntryResponse> entries(@RequestParam UUID settlementId) {
        return ledgerService.entriesForSettlement(settlementId);
    }
}
