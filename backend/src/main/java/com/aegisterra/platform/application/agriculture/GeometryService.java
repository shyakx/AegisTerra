package com.aegisterra.platform.application.agriculture;

import com.aegisterra.platform.infrastructure.persistence.platform.ConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeometryService {

    private final ObjectMapper objectMapper;
    private final ConfigurationRepository configurationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public GeometryService(ObjectMapper objectMapper, ConfigurationRepository configurationRepository) {
        this.objectMapper = objectMapper;
        this.configurationRepository = configurationRepository;
    }

    public record GeometryValidationResult(
        boolean valid,
        String reason,
        BigDecimal areaHa,
        MultiPolygon geometry,
        String geoJson
    ) {}

    public GeometryValidationResult validateGeoJson(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GeoJSON is required");
        }
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.path("type").asText("");
            if (!"Polygon".equalsIgnoreCase(type) && !"MultiPolygon".equalsIgnoreCase(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Polygon or MultiPolygon GeoJSON is supported");
            }

            Object[] row = (Object[]) entityManager.createNativeQuery("""
                SELECT
                  ST_IsValid(g) AS is_valid,
                  ST_IsValidReason(g) AS reason,
                  ST_Area(g::geography) / 10000.0 AS area_ha,
                  ST_AsText(ST_Multi(g)) AS wkt
                FROM (
                  SELECT ST_SetSRID(ST_GeomFromGeoJSON(:geojson), 4326) AS g
                ) q
                """)
                .setParameter("geojson", geoJson)
                .getSingleResult();

            boolean valid = Boolean.TRUE.equals(row[0]);
            String reason = row[1] != null ? row[1].toString() : null;
            BigDecimal areaHa = row[2] == null
                ? BigDecimal.ZERO
                : new BigDecimal(row[2].toString()).setScale(4, RoundingMode.HALF_UP);
            MultiPolygon geometry = parseWktMultiPolygon(row[3] != null ? row[3].toString() : null);

            if (geometry == null || geometry.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geometry is empty");
            }
            if (!geometry.isSimple() || selfIntersects(geometry)) {
                valid = false;
                reason = reason == null ? "Self-intersecting polygon" : reason + "; self-intersecting";
            }

            BigDecimal minArea = configDecimal("farm.boundary.min_area_ha", new BigDecimal("0.01"));
            BigDecimal maxArea = configDecimal("farm.boundary.max_area_ha", new BigDecimal("500"));
            if (valid && areaHa.compareTo(minArea) < 0) {
                valid = false;
                reason = "Area below minimum " + minArea + " ha";
            }
            if (valid && areaHa.compareTo(maxArea) > 0) {
                valid = false;
                reason = "Area above maximum " + maxArea + " ha";
            }

            return new GeometryValidationResult(valid, reason, areaHa, geometry, geoJson);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GeoJSON: " + ex.getMessage());
        }
    }

    public MultiPolygon requireValidGeometry(String geoJson) {
        GeometryValidationResult result = validateGeoJson(geoJson);
        if (!result.valid()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                result.reason() != null ? result.reason() : "Invalid geometry");
        }
        return result.geometry();
    }

    private boolean selfIntersects(Geometry geometry) {
        IsValidOp op = new IsValidOp(geometry);
        return !op.isValid();
    }

    private MultiPolygon parseWktMultiPolygon(String wkt) throws ParseException {
        if (wkt == null || wkt.isBlank()) {
            return null;
        }
        Geometry geometry = new WKTReader().read(wkt);
        geometry.setSRID(4326);
        if (geometry instanceof MultiPolygon multiPolygon) {
            return multiPolygon;
        }
        if (geometry instanceof Polygon polygon) {
            return geometry.getFactory().createMultiPolygon(new Polygon[]{polygon});
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geometry must resolve to MultiPolygon");
    }

    private BigDecimal configDecimal(String key, BigDecimal fallback) {
        return configurationRepository.findByConfigKeyAndDeletedFalse(key)
            .map(cfg -> {
                String raw = cfg.getValueJson().trim();
                if (raw.startsWith("\"")) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                try {
                    return new BigDecimal(raw);
                } catch (NumberFormatException ex) {
                    return fallback;
                }
            })
            .orElse(fallback);
    }

    public int draftExpiryDays() {
        return configurationRepository.findByConfigKeyAndDeletedFalse("registration.draft.expiry_days")
            .map(cfg -> {
                String raw = cfg.getValueJson().trim().replace("\"", "");
                try {
                    return Integer.parseInt(raw);
                } catch (NumberFormatException ex) {
                    return 30;
                }
            })
            .orElse(30);
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        return phone.trim().replace(" ", "");
    }

    public static void requireNationalId(String nationalId) {
        if (nationalId == null || !nationalId.matches("\\d{16}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "National ID must be 16 digits");
        }
    }

    public static void requirePhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null || !normalized.matches("(\\+?250)?0?7\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number format is invalid");
        }
    }

    public static String generateCode(String prefix) {
        String token = UUIDish();
        return prefix + "-" + token;
    }

    private static String UUIDish() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }
}
