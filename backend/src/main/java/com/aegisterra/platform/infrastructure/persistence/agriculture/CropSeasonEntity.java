package com.aegisterra.platform.infrastructure.persistence.agriculture;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "crop_seasons")
public class CropSeasonEntity extends AuditableEntity {

    @Column(name = "farm_id", nullable = false)
    private UUID farmId;

    @Column(name = "plot_id")
    private UUID plotId;

    @Column(name = "crop_id", nullable = false)
    private UUID cropId;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Column(name = "planted_area_ha")
    private BigDecimal plantedAreaHa;

    public UUID getFarmId() {
        return farmId;
    }

    public void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }

    public UUID getPlotId() {
        return plotId;
    }

    public void setPlotId(UUID plotId) {
        this.plotId = plotId;
    }

    public UUID getCropId() {
        return cropId;
    }

    public void setCropId(UUID cropId) {
        this.cropId = cropId;
    }

    public UUID getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(UUID seasonId) {
        this.seasonId = seasonId;
    }

    public BigDecimal getPlantedAreaHa() {
        return plantedAreaHa;
    }

    public void setPlantedAreaHa(BigDecimal plantedAreaHa) {
        this.plantedAreaHa = plantedAreaHa;
    }
}
