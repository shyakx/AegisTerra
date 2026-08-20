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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DecisionEngineIntegrationTest extends SharedPostgresContainer {

    private static final String GRAPH = """
        {
          "initialStep": "START",
          "terminalSteps": ["DONE", "REJECTED", "INFO"],
          "steps": [
            {"code":"START","name":"Start"},
            {
              "code":"REVIEW",
              "name":"Review",
              "task":{
                "enabled":true,
                "taskType":"GENERIC",
                "title":"Review subject",
                "assignees":[{"strategy":"ROLE","value":"SYSTEM_ADMIN"}],
                "allowedDecisions":["APPROVE","REJECT","REQUEST_INFORMATION","CANCEL"]
              }
            },
            {"code":"DONE","name":"Done"},
            {"code":"REJECTED","name":"Rejected"},
            {"code":"INFO","name":"Information"}
          ],
          "transitions": [
            {"from":"START","to":"REVIEW","action":"SUBMIT"},
            {"from":"REVIEW","to":"DONE","action":"APPROVE"},
            {"from":"REVIEW","to":"REJECTED","action":"REJECT"},
            {"from":"REVIEW","to":"INFO","action":"REQUEST_INFORMATION"}
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void decisionTypesAreConfigurable() throws Exception {
        MockHttpServletResponse login = login();
        mockMvc.perform(get("/api/v1/decisions/types").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(10)))
            .andExpect(jsonPath("$[?(@.code=='APPROVE')]").exists())
            .andExpect(jsonPath("$[?(@.code=='REJECT')]").exists())
            .andExpect(jsonPath("$[?(@.code=='DELEGATE')]").exists());
    }

    @Test
    void approveDecisionRoutesAndStoresHistory() throws Exception {
        MockHttpServletResponse login = login();
        String taskId = prepareReviewTask(login);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/decisions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisionTypeCode\":\"APPROVE\",\"comment\":\"looks good\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decisionTypeCode").value("APPROVE"))
            .andExpect(jsonPath("$.outcomeCode").value("APPROVED"))
            .andExpect(jsonPath("$.effect").value("COMPLETE_AND_ADVANCE"))
            .andExpect(jsonPath("$.workflowAction").value("APPROVE"));

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/decisions").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].decisionTypeCode").value("APPROVE"));

        mockMvc.perform(get("/api/v1/tasks/" + taskId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.timeline[?(@.eventType=='DECISION')]").exists());
    }

    @Test
    void rejectDecisionRoutesToRejectedTerminal() throws Exception {
        MockHttpServletResponse login = login();
        String taskId = prepareReviewTask(login);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        MvcResult decision = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/decisions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisionTypeCode\":\"REJECT\",\"comment\":\"insufficient evidence\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcomeCode").value("REJECTED"))
            .andReturn();
        String instanceId = objectMapper.readTree(decision.getResponse().getContentAsString())
            .get("instanceId").asText();

        mockMvc.perform(get("/api/v1/workflows/instances/" + instanceId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.currentStepCode").value("REJECTED"));
    }

    @Test
    void invalidDecisionRejectedWithoutHistory() throws Exception {
        MockHttpServletResponse login = login();
        String taskId = prepareReviewTask(login);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/decisions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisionTypeCode\":\"SKIP\"}"))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/decisions").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectWithoutCommentFailsValidation() throws Exception {
        MockHttpServletResponse login = login();
        String taskId = prepareReviewTask(login);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/decisions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisionTypeCode\":\"REJECT\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    private String prepareReviewTask(MockHttpServletResponse login) throws Exception {
        String code = "DEC_FLOW_" + System.nanoTime();
        MvcResult def = mockMvc.perform(post("/api/v1/workflows/definitions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"Decision flow","graphJson":%s}
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
            .andExpect(status().isOk());

        MvcResult inbox = mockMvc.perform(get("/api/v1/tasks")
                .param("status", "PENDING")
                .param("subjectType", "GENERIC_SUBJECT")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)))
            .andReturn();
        JsonNode content = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content");
        for (JsonNode task : content) {
            if (instanceId.equals(task.get("instanceId").asText())) {
                return task.get("id").asText();
            }
        }
        return content.get(0).get("id").asText();
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
