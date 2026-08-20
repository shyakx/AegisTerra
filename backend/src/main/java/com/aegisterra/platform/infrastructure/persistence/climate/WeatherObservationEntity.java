package com.aegisterra.platform.infrastructure.persistence.climate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weather_observations")
public class WeatherObservationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "temperature_c", precision = 6, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "rainfall_mm", precision = 10, scale = 2)
    private BigDecimal rainfallMm;

    @Column(name = "humidity_pct", precision = 5, scale = 2)
    private BigDecimal humidityPct;

    @Column(name = "wind_speed_ms", precision = 8, scale = 2)
    private BigDecimal windSpeedMs;

    @Column(length = 64)
    private String source;

    @Column(name = "variable_code", length = 64)
    private String variableCode;

    @Column(precision = 18, scale = 6)
    private BigDecimal value;

    @Column(length = 32)
    private String unit;

    @Column(name = "quality_flag", length = 32)
    private String qualityFlag;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "import_job_id")
    private UUID importJobId;

    @Column(name = "source_payload_hash", length = 128)
    private String sourcePayloadHash;

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
    public UUID getStationId() { return stationId; }
    public void setStationId(UUID stationId) { this.stationId = stationId; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }
    public BigDecimal getRainfallMm() { return rainfallMm; }
    public void setRainfallMm(BigDecimal rainfallMm) { this.rainfallMm = rainfallMm; }
    public BigDecimal getHumidityPct() { return humidityPct; }
    public void setHumidityPct(BigDecimal humidityPct) { this.humidityPct = humidityPct; }
    public BigDecimal getWindSpeedMs() { return windSpeedMs; }
    public void setWindSpeedMs(BigDecimal windSpeedMs) { this.windSpeedMs = windSpeedMs; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getVariableCode() { return variableCode; }
    public void setVariableCode(String variableCode) { this.variableCode = variableCode; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(String qualityFlag) { this.qualityFlag = qualityFlag; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public UUID getImportJobId() { return importJobId; }
    public void setImportJobId(UUID importJobId) { this.importJobId = importJobId; }
    public String getSourcePayloadHash() { return sourcePayloadHash; }
    public void setSourcePayloadHash(String sourcePayloadHash) { this.sourcePayloadHash = sourcePayloadHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
