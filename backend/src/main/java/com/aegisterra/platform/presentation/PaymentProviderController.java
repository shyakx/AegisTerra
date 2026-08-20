package com.aegisterra.platform.presentation;

import com.aegisterra.platform.application.settlement.PaymentProviderConfigService;
import com.aegisterra.platform.application.contracts.PaymentProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-providers")
@Tag(name = "Payment Providers")
public class PaymentProviderController {

    private final PaymentProviderConfigService configService;

    public PaymentProviderController(PaymentProviderConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settlements:read') or hasAuthority('settlements:process')")
    @Operation(summary = "List configured payment providers")
    public List<PaymentProviderResponse> list() {
        return configService.list();
    }
}
