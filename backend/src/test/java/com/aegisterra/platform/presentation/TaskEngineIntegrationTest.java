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
class TaskEngineIntegrationTest extends SharedPostgresContainer {

    private static final String GRAPH = """
        {
          "initialStep": "START",
          "terminalSteps": ["DONE"],
          "steps": [
            {"code":"START","name":"Start"},
            {
              "code":"REVIEW",
              "name":"Review",
              "task":{
                "enabled":true,
                "taskType":"GENERIC",
                "title":"Review subject",
                "assignees":[{"strategy":"ROLE","value":"SYSTEM_ADMIN"}]
              }
            },
            {"code":"DONE","name":"Done"}
          ],
          "transitions": [
            {"from":"START","to":"REVIEW","action":"SUBMIT"},
            {"from":"REVIEW","to":"DONE","action":"APPROVE"}
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rolePoolClaimCompleteAndAdvance() throws Exception {
        MockHttpServletResponse login = login();
        String code = "TASK_FLOW_" + System.nanoTime();

        MvcResult def = mockMvc.perform(post("/api/v1/workflows/definitions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"Task flow","graphJson":%s}
                    """.formatted(code, objectMapper.writeValueAsString(GRAPH))))
            .andExpect(status().isCreated())
            .andReturn();
        String definitionId = objectMapper.readTree(def.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/definitions/" + definitionId + "/publish")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        MvcResult start = mockMvc.perform(post("/api/v1/workflows/instances")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"definitionId":"%s","subjectType":"GENERIC_SUBJECT","subjectId":"%s"}
                    """.formatted(definitionId, UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn();
        String instanceId = objectMapper.readTree(start.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/workflows/instances/" + instanceId + "/transition")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"SUBMIT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStepCode").value("REVIEW"));

        mockMvc.perform(get("/api/v1/tasks/my").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        MvcResult inbox = mockMvc.perform(get("/api/v1/tasks")
                .param("status", "PENDING")
                .param("subjectType", "GENERIC_SUBJECT")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
            .andReturn();
        JsonNode task = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content").get(0);
        String taskId = task.get("id").asText();

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"APPROVED\",\"advanceAction\":\"APPROVE\",\"reason\":\"ok\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/workflows/instances/" + instanceId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/tasks/" + taskId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignments.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.timeline.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void tasksRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletResponse login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin@1234!Aa\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    }
}
