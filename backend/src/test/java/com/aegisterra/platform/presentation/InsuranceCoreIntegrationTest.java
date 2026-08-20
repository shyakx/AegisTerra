package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InsuranceCoreIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void productCatalogQuoteAndPolicyLifecycle() throws Exception {
        MockHttpServletResponse login = login();

        mockMvc.perform(get("/api/v1/insurance-products").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        String nationalId = "1196" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "073" + String.format("%07d", System.nanoTime() % 10_000_000);
        MvcResult farmerResult = mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Grace","lastName":"Uwimana","nationalId":"%s","phoneNumber":"%s"}
                    """.formatted(nationalId, phone)))
            .andExpect(status().isCreated())
            .andReturn();
        String farmerId = objectMapper.readTree(farmerResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult farmResult = mockMvc.perform(post("/api/v1/farms")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmerId":"%s","farmName":"Insurance Farm %d","farmSizeHa":2.5}
                    """.formatted(farmerId, System.nanoTime())))
            .andExpect(status().isCreated())
            .andReturn();
        String farmId = objectMapper.readTree(farmResult.getResponse().getContentAsString()).get("id").asText();

        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[30.1,-1.96],[30.101,-1.96],[30.101,-1.959],[30.1,-1.959],[30.1,-1.96]]]}";
        mockMvc.perform(post("/api/v1/farm-boundaries")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmId":"%s","geoJson":%s,"status":"ACTIVE"}
                    """.formatted(farmId, objectMapper.writeValueAsString(geoJson))))
            .andExpect(status().isCreated());

        String productId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001";
        String typeId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002";
        String packageId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003";

        MvcResult quoteResult = mockMvc.perform(post("/api/v1/premiums/quote")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"productId":"%s","coveragePackageId":"%s","farmerId":"%s","farmId":"%s","areaHa":2.5,"riskZoneCode":"MEDIUM"}
                    """.formatted(productId, packageId, farmerId, farmId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.netAmount").isNumber())
            .andReturn();
        JsonNode quote = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
        String quoteId = quote.get("id").asText();

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(6);
        MvcResult policyResult = mockMvc.perform(post("/api/v1/policies")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "farmerId":"%s","farmId":"%s","productId":"%s","policyTypeId":"%s",
                      "coveragePackageId":"%s","premiumQuoteId":"%s",
                      "startDate":"%s","endDate":"%s"
                    }
                    """.formatted(farmerId, farmId, productId, typeId, packageId, quoteId, start, end)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
            .andReturn();
        String policyId = objectMapper.readTree(policyResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/approve")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"underwriting-ok\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PREMIUM_PENDING"));

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/mark-premium-paid")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"paid\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/policies/" + policyId + "/documents").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)));

        mockMvc.perform(get("/api/v1/insurance/reports/active-policies").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(1)));
    }

    @Test
    void listPoliciesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/policies")).andExpect(status().isUnauthorized());
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
