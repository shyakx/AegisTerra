package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
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
class ExecutiveOverviewIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void overviewReturnsKpisAfterLogin() throws Exception {
        MockHttpServletResponse login = login();

        mockMvc.perform(get("/api/v1/executive/overview").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generatedAt").exists())
            .andExpect(jsonPath("$.farmers.total", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.farms.total", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.policies.total", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.claims.total", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.settlements.total", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.climate.stations", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.climate.openAlerts", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.tasks.pending", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.notifications.unread", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.quickLinks").isArray());

        mockMvc.perform(get("/api/v1/executive/regional-summary").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districts").isArray());
    }

    @Test
    void overviewRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/executive/overview")).andExpect(status().isUnauthorized());
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
