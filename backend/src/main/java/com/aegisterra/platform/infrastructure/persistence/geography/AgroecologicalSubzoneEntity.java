package com.aegisterra.platform.infrastructure.persistence.geography;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "agroecological_subzones")
public class AgroecologicalSubzoneEntity extends AuditableEntity {

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    public UUID getZoneId() {
        return zoneId;
    }

    public void setZoneId(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
