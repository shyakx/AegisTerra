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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgricultureIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationDraftValidateBoundaryAndSubmit() throws Exception {
        MockHttpServletResponse login = login();
        long n = System.nanoTime();
        String nationalId = "1197" + String.format("%012d", n % 1_000_000_000_000L);
        String phone = "072" + String.format("%07d", n % 10_000_000);
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[30.05,-1.95],[30.051,-1.95],[30.051,-1.949],[30.05,-1.949],[30.05,-1.95]]]}";

        mockMvc.perform(post("/api/v1/farm-boundaries/validate")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"geoJson\":" + objectMapper.writeValueAsString(geoJson) + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));

        String payload = """
            {
              "household":{"code":"HH-%d","headName":"Head"},
              "farmer":{"firstName":"Paul","lastName":"Nkurunziza","nationalId":"%s","phoneNumber":"%s"},
              "farm":{"farmName":"Valley Farm %d","farmSizeHa":1.2},
              "boundary":{"geoJson":%s},
              "plots":[{"plotCode":"P1","name":"Plot 1"}],
              "cropSeasons":[]
            }
            """.formatted(n, nationalId, phone, n, objectMapper.writeValueAsString(geoJson));

        MvcResult draftResult = mockMvc.perform(post("/api/v1/registration-drafts")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentStep\":8,\"payloadJson\":" + objectMapper.writeValueAsString(payload) + "}"))
            .andExpect(status().isCreated())
            .andReturn();

        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult submit = mockMvc.perform(post("/api/v1/registration-drafts/" + draftId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.farmerId").isNotEmpty())
            .andExpect(jsonPath("$.farmId").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(submit.getResponse().getContentAsString());
        mockMvc.perform(get("/api/v1/farmers/" + body.get("farmerId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Paul"));
        mockMvc.perform(get("/api/v1/farm-boundaries").param("farmId", body.get("farmId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
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
