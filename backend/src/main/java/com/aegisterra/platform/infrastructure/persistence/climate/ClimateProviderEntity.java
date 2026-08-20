package com.aegisterra.platform.infrastructure.persistence.climate;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "climate_providers")
public class ClimateProviderEntity extends AuditableEntity {
    @Column(nullable = false, length = 64)
    private String code;
    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "capabilities_json", columnDefinition = "text")
    private String capabilitiesJson;
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;
    @Column(name = "timezone_default", length = 64)
    private String timezoneDefault;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public void setCapabilitiesJson(String capabilitiesJson) { this.capabilitiesJson = capabilitiesJson; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public String getTimezoneDefault() { return timezoneDefault; }
    public void setTimezoneDefault(String timezoneDefault) { this.timezoneDefault = timezoneDefault; }
}
