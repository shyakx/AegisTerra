package com.aegisterra.platform.infrastructure.persistence.climate;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "climate_raster_layers")
public class ClimateRasterLayerEntity extends AuditableEntity {
    @Column(nullable = false, length = 64)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "provider_code", length = 64)
    private String providerCode;
    @Column(name = "variable_code", length = 64)
    private String variableCode;
    @Column(length = 64)
    private String crs;
    @Column(name = "resolution_m", precision = 12, scale = 2)
    private BigDecimal resolutionM;
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon bbox;
    @Column(name = "observed_at")
    private Instant observedAt;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_to")
    private Instant validTo;
    @Column(name = "storage_uri", length = 1000)
    private String storageUri;
    @Column(length = 32)
    private String format;
    @Column(length = 128)
    private String checksum;
    @Column(name = "byte_size")
    private Long byteSize;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getVariableCode() { return variableCode; }
    public void setVariableCode(String variableCode) { this.variableCode = variableCode; }
    public String getCrs() { return crs; }
    public void setCrs(String crs) { this.crs = crs; }
    public BigDecimal getResolutionM() { return resolutionM; }
    public void setResolutionM(BigDecimal resolutionM) { this.resolutionM = resolutionM; }
    public Polygon getBbox() { return bbox; }
    public void setBbox(Polygon bbox) { this.bbox = bbox; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }
    public String getStorageUri() { return storageUri; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Long getByteSize() { return byteSize; }
    public void setByteSize(Long byteSize) { this.byteSize = byteSize; }
}
