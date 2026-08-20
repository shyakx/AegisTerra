package com.aegisterra.platform.infrastructure.persistence.settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentProviderConfigRepository extends JpaRepository<PaymentProviderConfigEntity, UUID> {
    Optional<PaymentProviderConfigEntity> findByProviderCodeAndDeletedFalse(String providerCode);

    List<PaymentProviderConfigEntity> findByDeletedFalseOrderByProviderCodeAsc();

    List<PaymentProviderConfigEntity> findByEnabledTrueAndDeletedFalseOrderByProviderCodeAsc();
}
