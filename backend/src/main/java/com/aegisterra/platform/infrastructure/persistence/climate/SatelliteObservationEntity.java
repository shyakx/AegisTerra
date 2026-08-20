package com.aegisterra.platform.infrastructure.persistence.climate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.MultiPolygon;

@Entity
@Table(name = "satellite_observations")
public class SatelliteObservationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "product_id", nullable = false, length = 128)
    private String productId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "cloud_cover_pct", precision = 5, scale = 2)
    private BigDecimal cloudCoverPct;

    @Column(name = "storage_uri", length = 1000)
    private String storageUri;

    @Column(columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon footprint;

    @Column(name = "product_version", length = 64)
    private String productVersion;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "quality_flag", length = 32)
    private String qualityFlag;

    @Column(name = "import_job_id")
    private UUID importJobId;

    @Column(name = "dataset_id")
    private UUID datasetId;

    @Column(name = "acquired_at")
    private Instant acquiredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (qualityFlag == null) {
            qualityFlag = "RAW";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getFarmId() { return farmId; }
    public void setFarmId(UUID farmId) { this.farmId = farmId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public BigDecimal getCloudCoverPct() { return cloudCoverPct; }
    public void setCloudCoverPct(BigDecimal cloudCoverPct) { this.cloudCoverPct = cloudCoverPct; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public MultiPolygon getFootprint() { return footprint; }
    public void setFootprint(MultiPolygon footprint) { this.footprint = footprint; }
    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(String qualityFlag) { this.qualityFlag = qualityFlag; }
    public UUID getImportJobId() { return importJobId; }
    public void setImportJobId(UUID importJobId) { this.importJobId = importJobId; }
    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public Instant getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(Instant acquiredAt) { this.acquiredAt = acquiredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
