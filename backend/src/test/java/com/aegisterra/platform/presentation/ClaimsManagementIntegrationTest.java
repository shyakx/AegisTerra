package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimsManagementIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void draftEvidenceSubmitAdjudicateToPaymentPending() throws Exception {
        MockHttpServletResponse login = login();
        String policyId = createActivePolicy(login);

        MvcResult claimResult = mockMvc.perform(post("/api/v1/claims")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "policyId":"%s",
                      "claimTypeCode":"MANUAL",
                      "incidentDate":"%s",
                      "description":"Manual loss for integration test",
                      "claimedAmount":150000.00,
                      "causeOfLoss":"HAIL"
                    }
                    """.formatted(policyId, LocalDate.now())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.claimNumber", notNullValue()))
            .andReturn();
        JsonNode claim = objectMapper.readTree(claimResult.getResponse().getContentAsString());
        String claimId = claim.get("id").asText();

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/evidence")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"documentType":"CLAIM_FORM","title":"Claim form","storageUri":"file://claim-form.pdf","source":"TEST"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UNDER_VALIDATION"))
            .andExpect(jsonPath("$.workflowInstanceId", notNullValue()))
            .andExpect(jsonPath("$.workflowDefinitionCode").value("CLAIM_STANDARD"));

        decideOpenTask(login, claimId, "APPROVE", null);
        decideOpenTask(login, claimId, "SKIP", null);
        decideOpenTask(login, claimId, "APPROVE", null);
        decideOpenTask(login, claimId, "APPROVE", "approved for payment");

        mockMvc.perform(get("/api/v1/claims/" + claimId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", anyOf(is("APPROVED"), is("PAYMENT_PENDING"))));

        mockMvc.perform(get("/api/v1/claims/" + claimId + "/timeline").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/v1/claims/reports/by-status").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportCode").value("CLAIMS_BY_STATUS"));
    }

    @Test
    void listClaimsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/claims")).andExpect(status().isUnauthorized());
    }

    private void decideOpenTask(MockHttpServletResponse login, String claimId, String decision, String comment)
        throws Exception {
        MvcResult inbox = mockMvc.perform(get("/api/v1/tasks")
                .cookie(login.getCookie("at"))
                .param("subjectType", "CLAIM")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode content = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content");
        String taskId = null;
        for (JsonNode task : content) {
            if (claimId.equals(task.get("subjectId").asText())
                && ("PENDING".equals(task.get("status").asText())
                || "ASSIGNED".equals(task.get("status").asText())
                || "IN_PROGRESS".equals(task.get("status").asText()))) {
                taskId = task.get("id").asText();
                break;
            }
        }
        if (taskId == null) {
            inbox = mockMvc.perform(get("/api/v1/tasks/my")
                    .cookie(login.getCookie("at"))
                    .param("subjectType", "CLAIM"))
                .andExpect(status().isOk())
                .andReturn();
            content = objectMapper.readTree(inbox.getResponse().getContentAsString()).get("content");
            for (JsonNode task : content) {
                if (claimId.equals(task.get("subjectId").asText())) {
                    taskId = task.get("id").asText();
                    break;
                }
            }
        }
        if (taskId == null) {
            throw new AssertionError("No open CLAIM task for " + claimId);
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

    private String createActivePolicy(MockHttpServletResponse login) throws Exception {
        String nationalId = "1196" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "073" + String.format("%07d", System.nanoTime() % 10_000_000);
        MvcResult farmerResult = mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName":"Claim","lastName":"Tester","nationalId":"%s","phoneNumber":"%s"}
                    """.formatted(nationalId, phone)))
            .andExpect(status().isCreated())
            .andReturn();
        String farmerId = objectMapper.readTree(farmerResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult farmResult = mockMvc.perform(post("/api/v1/farms")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"farmerId":"%s","farmName":"Claims Farm %d","farmSizeHa":2.5}
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
            .andReturn();
        String quoteId = objectMapper.readTree(quoteResult.getResponse().getContentAsString()).get("id").asText();

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
            .andReturn();
        String policyId = objectMapper.readTree(policyResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/approve")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"underwriting-ok\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/policies/" + policyId + "/mark-premium-paid")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"paid\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        return policyId;
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
