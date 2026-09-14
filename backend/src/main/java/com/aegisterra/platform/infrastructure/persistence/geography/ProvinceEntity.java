package com.aegisterra.platform.infrastructure.persistence.geography;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Table(name = "provinces")
public class ProvinceEntity extends AuditableEntity {

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

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

    public MultiPolygon getGeom() {
        return geom;
    }

    public void setGeom(MultiPolygon geom) {
        this.geom = geom;
    }
}
