package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClimateDataPlatformIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void climateDashboardProvidersStationImportAndObservations() throws Exception {
        MockHttpServletResponse login = login();

        mockMvc.perform(get("/api/v1/climate/dashboard").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stationCount", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.enabledProviderCount", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/climate/providers").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/climate/stations").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[0].code").isNotEmpty());

        String code = "TST-" + (System.nanoTime() % 1_000_000);
        MvcResult stationResult = mockMvc.perform(post("/api/v1/climate/stations")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"Test Station","longitude":30.1,"latitude":-1.95,"providerCode":"MANUAL","districtCode":"GASABO"}
                    """.formatted(code)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.longitude").value(30.1))
            .andReturn();
        String stationId = objectMapper.readTree(stationResult.getResponse().getContentAsString()).get("id").asText();

        Instant t1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1, ChronoUnit.DAYS);
        String csv = "observed_at,variable_code,value,unit\n"
            + t1 + ",RAIN_MM,55.0,mm\n"
            + t1 + ",TEMP_C,24.0,C\n"
            + t2 + ",RAIN_MM,0.0,mm\n";

        mockMvc.perform(post("/api/v1/climate/import-jobs")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"providerCode":"MANUAL","jobType":"FILE_UPLOAD","stationCode":"%s","csvContent":%s}
                    """.formatted(code, objectMapper.writeValueAsString(csv))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.jobNumber", matchesPattern("CLIM-IMP-\\d{4}-\\d{9}")))
            .andExpect(jsonPath("$.rowsAccepted", greaterThanOrEqualTo(2)));

        Instant from = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.HOURS);
        mockMvc.perform(get("/api/v1/climate/observations")
                .cookie(login.getCookie("at"))
                .param("stationId", stationId)
                .param("from", from.toString())
                .param("to", to.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/v1/climate/map/stations").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void observationsRequireTimeWindow() throws Exception {
        MockHttpServletResponse login = login();
        mockMvc.perform(get("/api/v1/climate/observations").cookie(login.getCookie("at")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void climateRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/climate/dashboard")).andExpect(status().isUnauthorized());
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
