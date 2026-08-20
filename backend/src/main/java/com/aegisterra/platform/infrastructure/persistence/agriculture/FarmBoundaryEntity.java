package com.aegisterra.platform.infrastructure.persistence.agriculture;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Table(name = "farm_boundaries")
public class FarmBoundaryEntity extends AuditableEntity {

    @Column(name = "farm_id", nullable = false)
    private UUID farmId;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Column(length = 64)
    private String source;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "area_ha")
    private BigDecimal areaHa;

    public static FarmBoundaryEntity create(UUID farmId, MultiPolygon geom) {
        FarmBoundaryEntity boundary = new FarmBoundaryEntity();
        boundary.setFarmId(farmId);
        boundary.setGeom(geom);
        boundary.setCapturedAt(Instant.now());
        boundary.setStatus("DRAFT");
        boundary.setDeleted(false);
        return boundary;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public MultiPolygon getGeom() {
        return geom;
    }

    public void setGeom(MultiPolygon geom) {
        this.geom = geom;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public BigDecimal getAreaHa() {
        return areaHa;
    }

    public void setAreaHa(BigDecimal areaHa) {
        this.areaHa = areaHa;
    }
}
