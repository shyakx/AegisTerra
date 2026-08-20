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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SettlementPlatformIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void manualSettlementConfirmPostsLedgerAndReports() throws Exception {
        MockHttpServletResponse login = login();
        String sourceRecordId = UUID.randomUUID().toString();

        MvcResult created = mockMvc.perform(post("/api/v1/settlements")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceModule":"MANUAL",
                      "sourceRecordId":"%s",
                      "sourceReference":"OPS-TEST-1",
                      "amount":125000.00,
                      "currency":"RWF",
                      "paymentMethod":"MANUAL",
                      "providerCode":"MANUAL",
                      "beneficiaryName":"Settlement Farmer",
                      "beneficiaryAccount":"ACC-001"
                    }
                    """.formatted(sourceRecordId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.settlementNumber", matchesPattern("SET-\\d{4}-\\d{9}")))
            .andExpect(jsonPath("$.providerCode").value("MANUAL"))
            .andExpect(jsonPath("$.workflowDefinitionCode").value("SETTLEMENT_STANDARD"))
            .andExpect(jsonPath("$.status", anyOf(is("PENDING"), is("UNDER_REVIEW"))))
            .andReturn();

        JsonNode settlement = objectMapper.readTree(created.getResponse().getContentAsString());
        String settlementId = settlement.get("id").asText();

        // Drive finance/approval/disburse/confirm tasks when present
        for (int i = 0; i < 8; i++) {
            try {
                decideOpenTask(login, settlementId, "SETTLEMENT", "APPROVE", "ok");
            } catch (AssertionError ignored) {
                break;
            }
        }

        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/manual-confirm")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"providerReference":"MAN-IT-001","notes":"integration confirm"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.providerReference", notNullValue()));

        mockMvc.perform(get("/api/v1/settlements/" + settlementId + "/ledger").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/v1/settlements/" + settlementId + "/timeline").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/v1/settlements/reports/by-status").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportCode").value("SETTLEMENTS_BY_STATUS"));

        mockMvc.perform(get("/api/v1/payment-providers").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void settlementsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/settlements")).andExpect(status().isUnauthorized());
    }

    private void decideOpenTask(MockHttpServletResponse login, String subjectId, String subjectType,
                                String decision, String comment) throws Exception {
        MvcResult inbox = mockMvc.perform(get("/api/v1/tasks")
                .cookie(login.getCookie("at"))
                .param("subjectType", subjectType)
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode content = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content");
        String taskId = null;
        if (content != null) {
            for (JsonNode task : content) {
                if (subjectId.equals(task.get("subjectId").asText())) {
                    taskId = task.get("id").asText();
                    break;
                }
            }
        }
        if (taskId == null) {
            inbox = mockMvc.perform(get("/api/v1/tasks/my")
                    .cookie(login.getCookie("at"))
                    .param("subjectType", subjectType))
                .andExpect(status().isOk())
                .andReturn();
            content = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content");
            if (content != null) {
                for (JsonNode task : content) {
                    if (subjectId.equals(task.get("subjectId").asText())) {
                        taskId = task.get("id").asText();
                        break;
                    }
                }
            }
        }
        if (taskId == null) {
            throw new AssertionError("No open " + subjectType + " task for " + subjectId);
        }

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim").cookie(login.getCookie("at")))
            .andExpect(status().isOk());

        String body = comment == null
            ? "{\"decisionTypeCode\":\"%s\"}".formatted(decision)
            : "{\"decisionTypeCode\":\"%s\",\"comment\":\"%s\"}".formatted(decision, comment);
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/decisions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
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
