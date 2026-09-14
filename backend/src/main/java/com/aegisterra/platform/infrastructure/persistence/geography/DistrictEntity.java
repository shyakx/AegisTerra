package com.aegisterra.platform.infrastructure.persistence.geography;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Table(name = "districts")
public class DistrictEntity extends AuditableEntity {

    @Column(name = "province_id")
    private UUID provinceId;

    @Column(name = "agroecological_subzone_id")
    private UUID agroecologicalSubzoneId;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    public UUID getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(UUID provinceId) {
        this.provinceId = provinceId;
    }

    public UUID getAgroecologicalSubzoneId() {
        return agroecologicalSubzoneId;
    }

    public void setAgroecologicalSubzoneId(UUID agroecologicalSubzoneId) {
        this.agroecologicalSubzoneId = agroecologicalSubzoneId;
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

    public MultiPolygon getGeom() {
        return geom;
    }

    public void setGeom(MultiPolygon geom) {
        this.geom = geom;
    }
}
