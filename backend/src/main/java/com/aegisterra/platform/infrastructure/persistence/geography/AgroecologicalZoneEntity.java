package com.aegisterra.platform.infrastructure.persistence.geography;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "agroecological_zones")
public class AgroecologicalZoneEntity extends AuditableEntity {

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

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
