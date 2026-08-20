package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.application.settlement.spi.PaymentProvider;
import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderConfigEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderConfigRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProvider> providers;
    private final PaymentProviderConfigRepository configRepository;

    public PaymentProviderRegistry(List<PaymentProvider> providers, PaymentProviderConfigRepository configRepository) {
        this.providers = providers.stream()
            .collect(Collectors.toMap(p -> p.code().toUpperCase(Locale.ROOT), Function.identity(), (a, b) -> a));
        this.configRepository = configRepository;
    }

    public PaymentProvider require(String code) {
        return requireEnabled(code);
    }

    public PaymentProvider requireEnabled(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider code is required");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        PaymentProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown payment provider: " + code);
        }
        PaymentProviderConfigEntity config = configRepository.findByProviderCodeAndDeletedFalse(normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Payment provider config missing: " + code));
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Payment provider is disabled: " + code);
        }
        return provider;
    }

    public List<PaymentProvider> all() {
        return List.copyOf(providers.values());
    }
}
