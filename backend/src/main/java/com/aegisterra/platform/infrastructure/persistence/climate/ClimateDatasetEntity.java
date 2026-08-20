package com.aegisterra.platform.infrastructure.persistence.climate;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "climate_datasets")
public class ClimateDatasetEntity extends AuditableEntity {
    @Column(nullable = false, length = 64)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(name = "dataset_type", nullable = false, length = 64)
    private String datasetType;
    @Column(name = "time_start")
    private Instant timeStart;
    @Column(name = "time_end")
    private Instant timeEnd;
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon bbox;
    @Column(name = "provider_code", length = 64)
    private String providerCode;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDatasetType() { return datasetType; }
    public void setDatasetType(String datasetType) { this.datasetType = datasetType; }
    public Instant getTimeStart() { return timeStart; }
    public void setTimeStart(Instant timeStart) { this.timeStart = timeStart; }
    public Instant getTimeEnd() { return timeEnd; }
    public void setTimeEnd(Instant timeEnd) { this.timeEnd = timeEnd; }
    public Polygon getBbox() { return bbox; }
    public void setBbox(Polygon bbox) { this.bbox = bbox; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
}
