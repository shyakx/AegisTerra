package com.aegisterra.platform.application.climate;

import com.aegisterra.platform.application.climate.spi.ClimateDataProvider;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateProviderEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.ClimateProviderRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClimateProviderRegistry {

    private final Map<String, ClimateDataProvider> providers;
    private final ClimateProviderRepository providerRepository;

    public ClimateProviderRegistry(List<ClimateDataProvider> providers, ClimateProviderRepository providerRepository) {
        this.providers = providers.stream()
            .collect(Collectors.toMap(p -> p.code().toUpperCase(Locale.ROOT), Function.identity()));
        this.providerRepository = providerRepository;
    }

    public ClimateDataProvider require(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider code is required");
        }
        ClimateDataProvider provider = providers.get(code.toUpperCase(Locale.ROOT));
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown climate provider: " + code);
        }
        ClimateProviderEntity config = providerRepository.findByCodeAndDeletedFalse(code.toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Climate provider is not registered: " + code));
        if (!config.isEnabled() && !"MANUAL".equalsIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Climate provider is disabled: " + code);
        }
        return provider;
    }

    public List<ClimateDataProvider> all() {
        return List.copyOf(providers.values());
    }
}
