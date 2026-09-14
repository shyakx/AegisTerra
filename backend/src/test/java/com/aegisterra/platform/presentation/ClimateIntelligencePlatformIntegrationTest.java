package com.aegisterra.platform.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationEntity;
import com.aegisterra.platform.infrastructure.persistence.climate.WeatherStationRepository;
import com.aegisterra.platform.support.SharedPostgresContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ClimateIntelligencePlatformIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WeatherStationRepository weatherStationRepository;

    private static final Pattern UUID_STRING = Pattern.compile(
        "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    );

    @Test
    void importThenRecalculateProducesRiskAndDashboard() throws Exception {
        MockHttpServletResponse login = login();

        String nationalId = "1197" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "078" + String.format("%07d", System.nanoTime() % 10_000_000);
        MvcResult farmerResult = mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Climate","lastName":"Farmer","nationalId":"%s","phoneNumber":"%s"}
                    """.formatted(nationalId, phone)))
            .andExpect(status().isCreated())
            .andReturn();
        String farmerId = objectMapper.readTree(farmerResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult farmResult = mockMvc.perform(post("/api/v1/farms")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmerId":"%s","farmName":"Climate Farm %d","farmSizeHa":1.5}
                    """.formatted(farmerId, System.nanoTime())))
            .andExpect(status().isCreated())
            .andReturn();
        String farmId = objectMapper.readTree(farmResult.getResponse().getContentAsString()).get("id").asText();

        Instant t1 = Instant.now().minus(3, ChronoUnit.DAYS);
        String csv = "observed_at,variable_code,value,unit\n"
            + t1 + ",RAIN_MM,90.0,mm\n"
            + t1 + ",TEMP_C,28.0,C\n";
        mockMvc.perform(post("/api/v1/climate/import-jobs")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"providerCode":"MANUAL","jobType":"FILE_UPLOAD","stationCode":"KGL-01","csvContent":%s}
                    """.formatted(objectMapper.writeValueAsString(csv))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rowsAccepted", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/v1/climate-intel/jobs/recalculate")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"farmId\":\"%s\"}".formatted(farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.jobNumber", matchesPattern("CLT-JOB-\\d{4}-\\d{9}")))
            .andExpect(jsonPath("$.subjectsProcessed", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/climate-intel/farms/" + farmId + "/risk-score").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.farmId").value(farmId))
            .andExpect(jsonPath("$.grade").isString());

        mockMvc.perform(get("/api/v1/climate-intel/farms/" + farmId + "/profile").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshotJson").isString());

        mockMvc.perform(get("/api/v1/climate-intel/national/dashboard").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.farmsByGrade").isMap());

        mockMvc.perform(get("/api/v1/climate-intel/alerts").cookie(login.getCookie("at")))
            .andExpect(status().isOk());
    }

    @Test
    void climateIntelRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/climate-intel/national/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/climate-intel/aez/risk-summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void nullFarmDistrictUsesStationCodeNotUuid() throws Exception {
        MockHttpServletResponse login = login();
        String farmId = createFarm(login, null);
        importHeavyRain(login);
        recalculate(login, farmId);

        String districtCode = latestScoreDistrictCode(farmId);
        String stationCode = firstStationDistrictCode();
        assertThat(districtCode).isEqualTo(stationCode);
        assertThat(districtCode).doesNotMatch(UUID_STRING);
        assertThat(alertDistrictCodes(farmId)).isNotEmpty().allMatch(code -> code.equals(stationCode));
    }

    @Test
    void farmDistrictUuidResolvesToDistrictsCodeAndAlert() throws Exception {
        MockHttpServletResponse login = login();
        String musanzeId = musanzeDistrictId();
        String farmId = createFarm(login, musanzeId);
        importHeavyRain(login);
        recalculate(login, farmId);

        String districtCode = latestScoreDistrictCode(farmId);
        assertThat(districtCode).isEqualTo("MUSANZE");
        assertThat(districtCode).isNotEqualTo(musanzeId);
        assertThat(districtCode).doesNotMatch(UUID_STRING);
        assertThat(alertDistrictCodes(farmId)).isNotEmpty().containsOnly("MUSANZE");
    }

    @Test
    void unresolvedFarmDistrictFallsBackToStationCodeNotUuid() throws Exception {
        MockHttpServletResponse login = login();
        UUID districtId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO districts (
                id, code, name, province_id, agroecological_subzone_id,
                created_at, updated_at, version, status, deleted
            )
            SELECT ?, ?, 'Climate unresolved district', province_id, agroecological_subzone_id,
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'ACTIVE', FALSE
            FROM districts WHERE code = 'MUSANZE' AND deleted = false
            """,
            districtId,
            "CLIM-" + districtId.toString().substring(0, 8).toUpperCase()
        );
        String farmId = createFarm(login, districtId.toString());
        jdbcTemplate.update("UPDATE districts SET deleted = TRUE, status = 'DISABLED' WHERE id = ?", districtId);

        importHeavyRain(login);
        recalculate(login, farmId);

        String districtCode = latestScoreDistrictCode(farmId);
        assertThat(districtCode).isEqualTo(firstStationDistrictCode());
        assertThat(districtCode).isNotEqualTo(districtId.toString());
        assertThat(districtCode).doesNotMatch(UUID_STRING);
        assertThat(alertDistrictCodes(farmId)).isNotEmpty()
            .allMatch(code -> code.equals(firstStationDistrictCode()));
    }

    @Test
    void aezRiskSummaryAggregatesLatestMappedDistrictsAndExcludesUnmapped() throws Exception {
        MockHttpServletResponse login = login();

        // Timestamps after now so they beat demo-seed calculated_at values.
        Instant base = Instant.now().plus(1, ChronoUnit.HOURS);
        insertDistrictSnapshot("MUSANZE", 10.0, base.minus(30, ChronoUnit.MINUTES), 1);
        insertDistrictSnapshot("MUSANZE", 40.0, base, 2);
        insertDistrictSnapshot("BURERA", 60.0, base, 3);
        insertDistrictSnapshot("HUYE", 80.0, base, 4);
        insertDistrictSnapshot("KIGALI", 99.0, base, 5);
        insertDistrictSnapshot("ZZ-UNMAPPED-CODE", 5.0, base, 6);

        MvcResult result = mockMvc.perform(get("/api/v1/climate-intel/aez/risk-summary").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zones").isArray())
            .andExpect(jsonPath("$.subzones").isArray())
            .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());

        var zoneA = findByCode(body.get("zones"), "A");
        assertThat(zoneA.get("name").asText()).isEqualTo("ZONE A");
        assertThat(zoneA.get("meanScore").asDouble()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(zoneA.get("grade").asText()).isEqualTo("HIGH");
        assertThat(zoneA.get("districtCount").asInt()).isGreaterThanOrEqualTo(2);

        var zoneB = findByCode(body.get("zones"), "B");
        assertThat(zoneB.get("meanScore").asDouble()).isCloseTo(80.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(zoneB.get("grade").asText()).isEqualTo("EXTREME");

        var a1 = findByCode(body.get("subzones"), "A1");
        assertThat(a1.get("zoneCode").asText()).isEqualTo("A");
        assertThat(a1.get("name").asText()).isEqualTo("Volcanic Highlands");
        assertThat(a1.get("meanScore").asDouble()).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(a1.get("grade").asText()).isEqualTo("HIGH");
        assertThat(a1.get("districtCount").asInt()).isEqualTo(2);

        var unmapped = new java.util.ArrayList<String>();
        body.get("unmappedDistrictCodes").forEach(n -> unmapped.add(n.asText()));
        assertThat(unmapped).contains("KIGALI", "ZZ-UNMAPPED-CODE");
        assertThat(body.get("unmappedCount").asInt()).isEqualTo(unmapped.size());
        assertThat(unmapped).doesNotContain("MUSANZE", "BURERA", "HUYE");

        // Unmapped 99 must not pull Zone A mean above 50.
        assertThat(zoneA.get("meanScore").asDouble()).isLessThan(99.0);

        mockMvc.perform(get("/api/v1/climate-intel/national/dashboard").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zoneHeat").isArray())
            .andExpect(jsonPath("$.subzoneHeat").isArray())
            .andExpect(jsonPath("$.unmappedDistrictCodes").isArray());

        // National district heat must use latest-per-district (no historical MUSANZE duplicates).
        MvcResult dash = mockMvc.perform(get("/api/v1/climate-intel/national/dashboard").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andReturn();
        var dashBody = objectMapper.readTree(dash.getResponse().getContentAsString());
        long musanzeHeatRows = 0;
        for (var row : dashBody.get("districtHeat")) {
            if ("MUSANZE".equals(row.get("districtCode").asText())) {
                musanzeHeatRows++;
                assertThat(row.get("meanScore").asDouble()).isCloseTo(40.0, org.assertj.core.data.Offset.offset(0.0001));
            }
        }
        assertThat(musanzeHeatRows).isEqualTo(1);

        mockMvc.perform(get("/api/v1/climate-intel/districts/MUSANZE/risk-profile").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districtCode").value("MUSANZE"))
            .andExpect(jsonPath("$.provinceCode").value("NORTH"))
            .andExpect(jsonPath("$.agroecologicalZoneCode").value("A"))
            .andExpect(jsonPath("$.agroecologicalSubzoneCode").value("A1"));
    }

    @Test
    void yieldClimateOutlookReturnsPastRecentAndPredictedPerCrop() throws Exception {
        MockHttpServletResponse login = login();

        MvcResult result = mockMvc.perform(get("/api/v1/climate-intel/planning/yield-outlook").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.crops").isArray())
            .andExpect(jsonPath("$.methodology").isString())
            .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("crops").size()).isGreaterThanOrEqualTo(3);

        var maize = findByCropCode(body.get("crops"), "MAIZE");
        assertThat(maize.get("cropName").asText()).isEqualTo("Maize");
        assertThat(maize.get("pastMeanYieldTHa").asDouble()).isGreaterThan(0);
        assertThat(maize.get("recentMeanYieldTHa").asDouble()).isGreaterThan(0);
        assertThat(maize.get("predictedYieldTHa").asDouble()).isGreaterThan(0);
        assertThat(maize.get("yearlySeries").size()).isGreaterThanOrEqualTo(5);
        assertThat(maize.get("narrative").asText()).contains("Maize");
    }

    @Test
    void maizeMlSpikeReturnsFeaturesAndLeaveOneYearMetrics() throws Exception {
        MockHttpServletResponse login = login();

        MvcResult result = mockMvc.perform(get("/api/v1/climate-intel/planning/ml-spike/maize").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cropCode").value("MAIZE"))
            .andExpect(jsonPath("$.features").isArray())
            .andExpect(jsonPath("$.leaveOneYearOut").isArray())
            .andExpect(jsonPath("$.metrics.folds").isNumber())
            .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("featureRowCount").asInt()).isGreaterThanOrEqualTo(5);
        assertThat(body.get("metrics").get("bestMethod").asText()).isNotBlank();
        assertThat(body.get("verdict").asText()).isNotBlank();
        assertThat(body.get("features").get(0).has("lag1YieldTHa")).isTrue();
        assertThat(body.get("features").get(0).has("seasonRainMm")).isTrue();
    }

    @Test
    void yieldOutlookCsvAndDemoFarmsHaveDistrictsForPresentation() throws Exception {
        MockHttpServletResponse login = login();

        String csv = mockMvc.perform(get("/api/v1/climate-intel/planning/yield-outlook.csv").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(csv).contains("crop_code");
        assertThat(csv).contains("MAIZE");
        assertThat(csv).contains("predicted_yield_t_ha");

        Integer withDistrict = jdbcTemplate.queryForObject(
            """
            SELECT count(*)::int FROM farms
            WHERE deleted = false
              AND farm_code LIKE 'FARM-2026-%'
              AND district_id IS NOT NULL
            """,
            Integer.class
        );
        assertThat(withDistrict).isGreaterThanOrEqualTo(12);
    }

    private static com.fasterxml.jackson.databind.JsonNode findByCropCode(
        com.fasterxml.jackson.databind.JsonNode array,
        String code
    ) {
        for (com.fasterxml.jackson.databind.JsonNode node : array) {
            if (code.equals(node.get("cropCode").asText())) {
                return node;
            }
        }
        throw new AssertionError("Missing crop code " + code);
    }

    private void insertDistrictSnapshot(String districtCode, double score, Instant calculatedAt, int farmCount) {
        jdbcTemplate.update(
            """
            INSERT INTO district_risk_snapshots (
                id, district_code, score, grade, confidence, farm_count, open_alert_count,
                components_json, calculated_at, created_at, updated_at, version, status, deleted
            ) VALUES (
                ?, ?, ?, ?, 0.5, ?, 0, '{}', ?, ?, ?, 0, 'ACTIVE', FALSE
            )
            """,
            UUID.randomUUID(),
            districtCode,
            score,
            com.aegisterra.platform.domain.climateintelligence.RiskGrade.fromScore(score).name(),
            farmCount,
            java.sql.Timestamp.from(calculatedAt),
            java.sql.Timestamp.from(calculatedAt),
            java.sql.Timestamp.from(calculatedAt)
        );
    }

    private static com.fasterxml.jackson.databind.JsonNode findByCode(
        com.fasterxml.jackson.databind.JsonNode array,
        String code
    ) {
        for (com.fasterxml.jackson.databind.JsonNode node : array) {
            if (code.equals(node.get("code").asText())) {
                return node;
            }
        }
        throw new AssertionError("Missing AEZ code " + code);
    }

    private String createFarm(MockHttpServletResponse login, String districtId) throws Exception {
        String nationalId = "1197" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "078" + String.format("%07d", System.nanoTime() % 10_000_000);
        MvcResult farmerResult = mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Climate","lastName":"Farmer","nationalId":"%s","phoneNumber":"%s"}
                    """.formatted(nationalId, phone)))
            .andExpect(status().isCreated())
            .andReturn();
        String farmerId = objectMapper.readTree(farmerResult.getResponse().getContentAsString()).get("id").asText();

        String districtJson = districtId == null ? "" : ",\"districtId\":\"%s\"".formatted(districtId);
        MvcResult farmResult = mockMvc.perform(post("/api/v1/farms")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmerId":"%s","farmName":"Climate Farm %d","farmSizeHa":1.5%s}
                    """.formatted(farmerId, System.nanoTime(), districtJson)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(farmResult.getResponse().getContentAsString()).get("id").asText();
    }

    private void importHeavyRain(MockHttpServletResponse login) throws Exception {
        Instant t1 = Instant.now().minus(3, ChronoUnit.DAYS);
        String csv = "observed_at,variable_code,value,unit\n"
            + t1 + ",RAIN_MM,90.0,mm\n"
            + t1 + ",TEMP_C,28.0,C\n";
        mockMvc.perform(post("/api/v1/climate/import-jobs")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"providerCode":"MANUAL","jobType":"FILE_UPLOAD","stationCode":"%s","csvContent":%s}
                    """.formatted(firstStation().getCode(), objectMapper.writeValueAsString(csv))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rowsAccepted", greaterThanOrEqualTo(1)));
    }

    private void recalculate(MockHttpServletResponse login, String farmId) throws Exception {
        mockMvc.perform(post("/api/v1/climate-intel/jobs/recalculate")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"farmId\":\"%s\"}".formatted(farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subjectsProcessed", greaterThanOrEqualTo(1)));
    }

    private String latestScoreDistrictCode(String farmId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT district_code FROM risk_scores
            WHERE farm_id = ?::uuid AND deleted = false
            ORDER BY calculated_at DESC
            LIMIT 1
            """,
            String.class,
            farmId
        );
    }

    private List<String> alertDistrictCodes(String farmId) {
        return jdbcTemplate.queryForList(
            """
            SELECT district_code FROM climate_alerts
            WHERE farm_id = ?::uuid AND deleted = false
            """,
            String.class,
            farmId
        );
    }

    private String musanzeDistrictId() {
        return jdbcTemplate.queryForObject(
            "SELECT id::text FROM districts WHERE code = 'MUSANZE' AND deleted = false",
            String.class
        );
    }

    private WeatherStationEntity firstStation() {
        return weatherStationRepository.findByDeletedFalse().getFirst();
    }

    private String firstStationDistrictCode() {
        return firstStation().getDistrictCode();
    }

    private MockHttpServletResponse login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin@1234!Aa\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse();
    }
}
