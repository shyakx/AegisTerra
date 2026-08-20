package com.aegisterra.platform.infrastructure.persistence.climate;

import com.aegisterra.platform.infrastructure.persistence.identity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "weather_stations")
public class WeatherStationEntity extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Column(name = "elevation_m", precision = 10, scale = 2)
    private BigDecimal elevationM;

    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Column(name = "external_station_id", length = 128)
    private String externalStationId;

    @Column(name = "district_code", length = 64)
    private String districtCode;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Point getGeom() { return geom; }
    public void setGeom(Point geom) { this.geom = geom; }
    public BigDecimal getElevationM() { return elevationM; }
    public void setElevationM(BigDecimal elevationM) { this.elevationM = elevationM; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getExternalStationId() { return externalStationId; }
    public void setExternalStationId(String externalStationId) { this.externalStationId = externalStationId; }
    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }
}
