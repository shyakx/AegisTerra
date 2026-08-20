package com.aegisterra.platform.infrastructure.persistence.platform;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "configurations")
public class ConfigurationEntity extends AuditableEntity {

    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    @Column(nullable = false, length = 64)
    private String category;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "value_json", nullable = false, columnDefinition = "text")
    private String valueJson;

    @Column(length = 500)
    private String description;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValueJson() {
        return valueJson;
    }

    public void setValueJson(String valueJson) {
        this.valueJson = valueJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
