package com.aegisterra.platform.infrastructure.persistence.settlement;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_provider_config")
public class PaymentProviderConfigEntity extends AuditableEntity {

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "payment_method", nullable = false, length = 64)
    private String paymentMethod;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
}
