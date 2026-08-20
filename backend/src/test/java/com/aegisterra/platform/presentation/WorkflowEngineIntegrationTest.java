package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
class WorkflowEngineIntegrationTest extends SharedPostgresContainer {

    private static final String GRAPH_V1 = """
        {
          "initialStep": "START",
          "terminalSteps": ["DONE", "REJECTED"],
          "steps": [
            {"code":"START","name":"Start"},
            {"code":"REVIEW","name":"Review"},
            {"code":"DONE","name":"Done"},
            {"code":"REJECTED","name":"Rejected"}
          ],
          "transitions": [
            {"from":"START","to":"REVIEW","action":"SUBMIT"},
            {"from":"REVIEW","to":"DONE","action":"APPROVE"},
            {"from":"REVIEW","to":"REJECTED","action":"REJECT"}
          ]
        }
        """;

    private static final String GRAPH_V2 = """
        {
          "initialStep": "START",
          "terminalSteps": ["DONE"],
          "steps": [
            {"code":"START","name":"Start"},
            {"code":"DONE","name":"Done"}
          ],
          "transitions": [
            {"from":"START","to":"DONE","action":"FINISH"}
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void versioningRuntimeTransitionValidationAndTimeline() throws Exception {
        MockHttpServletResponse login = login();

        String code = "GENERIC_LINEAR_" + System.nanoTime();
        MvcResult create = mockMvc.perform(post("/api/v1/workflows/definitions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"Generic linear","description":"Stage 6A test","graphJson":%s}
                    """.formatted(code, objectMapper.writeValueAsString(GRAPH_V1))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versions.length()", greaterThanOrEqualTo(1)))
            .andReturn();
        String definitionId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/definitions/" + definitionId + "/publish")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishedVersionNo").value(1));

        UUID subjectId = UUID.randomUUID();
        MvcResult start = mockMvc.perform(post("/api/v1/workflows/instances")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"definitionId":"%s","subjectType":"GENERIC_SUBJECT","subjectId":"%s","correlationId":"corr-1"}
                    """.formatted(definitionId, subjectId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.currentStepCode").value("START"))
            .andExpect(jsonPath("$.definitionVersionNo").value(1))
            .andExpect(jsonPath("$.events.length()", greaterThanOrEqualTo(2)))
            .andReturn();
        JsonNode instance = objectMapper.readTree(start.getResponse().getContentAsString());
        String instanceId = instance.get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/instances/" + instanceId + "/transition")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"APPROVE\",\"reason\":\"too early\"}"))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/workflows/instances/" + instanceId + "/transition")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUBMIT\",\"reason\":\"submitted\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStepCode").value("REVIEW"))
            .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(post("/api/v1/workflows/instances/" + instanceId + "/transition")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"APPROVE\",\"reason\":\"ok\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStepCode").value("DONE"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.events.length()", greaterThanOrEqualTo(4)))
            .andExpect(jsonPath("$.transitions.length()", greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/api/v1/workflows/definitions/" + definitionId + "/versions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"graphJson\":" + objectMapper.writeValueAsString(GRAPH_V2) + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNo").value(2))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/v1/workflows/definitions/" + definitionId + "/publish")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishedVersionNo").value(2));

        mockMvc.perform(get("/api/v1/workflows/instances/" + instanceId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.definitionVersionNo").value(1))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        MvcResult startV2 = mockMvc.perform(post("/api/v1/workflows/instances")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"definitionId":"%s","subjectType":"GENERIC_SUBJECT","subjectId":"%s"}
                    """.formatted(definitionId, UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.definitionVersionNo").value(2))
            .andReturn();
        String instanceV2 = objectMapper.readTree(startV2.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/instances/" + instanceV2 + "/transition")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"FINISH\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void definitionsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/definitions")).andExpect(status().isUnauthorized());
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
