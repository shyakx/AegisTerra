package com.aegisterra.platform.infrastructure.persistence.agriculture;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "farms")
public class FarmEntity extends AuditableEntity {

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "farm_code", nullable = false, length = 64)
    private String farmCode;

    @Column(name = "farm_name", nullable = false)
    private String farmName;

    @Column(name = "farm_size_ha")
    private BigDecimal farmSizeHa;

    @Column(name = "crop_type", length = 100)
    private String cropType;

    @Column(name = "district_id")
    private UUID districtId;

    @Column(name = "sector_id")
    private UUID sectorId;

    @Column(name = "cell_id")
    private UUID cellId;

    @Column(name = "village_id")
    private UUID villageId;

    public static FarmEntity create(UUID farmerId, String farmCode, String farmName) {
        FarmEntity farm = new FarmEntity();
        farm.setFarmerId(farmerId);
        farm.setFarmCode(farmCode);
        farm.setFarmName(farmName);
        farm.setStatus("DRAFT");
        farm.setDeleted(false);
        return farm;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(UUID farmerId) {
        this.farmerId = farmerId;
    }

    public String getFarmCode() {
        return farmCode;
    }

    public void setFarmCode(String farmCode) {
        this.farmCode = farmCode;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public BigDecimal getFarmSizeHa() {
        return farmSizeHa;
    }

    public void setFarmSizeHa(BigDecimal farmSizeHa) {
        this.farmSizeHa = farmSizeHa;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public UUID getDistrictId() {
        return districtId;
    }

    public void setDistrictId(UUID districtId) {
        this.districtId = districtId;
    }

    public UUID getSectorId() {
        return sectorId;
    }

    public void setSectorId(UUID sectorId) {
        this.sectorId = sectorId;
    }

    public UUID getCellId() {
        return cellId;
    }

    public void setCellId(UUID cellId) {
        this.cellId = cellId;
    }

    public UUID getVillageId() {
        return villageId;
    }

    public void setVillageId(UUID villageId) {
        this.villageId = villageId;
    }
}
