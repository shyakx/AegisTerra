package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
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
class ClimateIntelligencePlatformIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
