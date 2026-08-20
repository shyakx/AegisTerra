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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FarmControllerTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listFarmsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/farms"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createFarmForExistingFarmer() throws Exception {
        MockHttpServletResponse login = login();

        String nationalId = "1198" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "079" + String.format("%07d", System.nanoTime() % 10_000_000);

        MvcResult farmerResult = mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Aline","lastName":"Uwase","nationalId":"%s","phoneNumber":"%s"}
                    """.formatted(nationalId, phone)))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode farmer = objectMapper.readTree(farmerResult.getResponse().getContentAsString());
        String farmerId = farmer.get("id").asText();

        mockMvc.perform(get("/api/v1/farms").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(0)));

        mockMvc.perform(post("/api/v1/farms")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmerId":"%s","farmName":"Hilltop Farm","farmSizeHa":4.5,"cropType":"MAIZE"}
                    """.formatted(farmerId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.farmName").value("Hilltop Farm"));
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
