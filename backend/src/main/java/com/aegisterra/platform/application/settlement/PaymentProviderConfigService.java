package com.aegisterra.platform.application.settlement;

import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderConfigEntity;
import com.aegisterra.platform.infrastructure.persistence.settlement.PaymentProviderConfigRepository;
import com.aegisterra.platform.application.contracts.PaymentProviderResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProviderConfigService {

    private final PaymentProviderConfigRepository repository;

    public PaymentProviderConfigService(PaymentProviderConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderResponse> list() {
        return repository.findByDeletedFalseOrderByProviderCodeAsc().stream()
            .map(this::toResponse)
            .toList();
    }

    private PaymentProviderResponse toResponse(PaymentProviderConfigEntity e) {
        return new PaymentProviderResponse(
            e.getId(),
            e.getProviderCode(),
            e.getDisplayName(),
            e.getPaymentMethod(),
            e.isEnabled(),
            e.getConfigJson(),
            e.getStatus()
        );
    }
}
