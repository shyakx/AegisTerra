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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationPlatformIntegrationTest extends SharedPostgresContainer {

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
                "title":"Review for notify",
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
    void workflowEventsCreateInAppNotifications() throws Exception {
        MockHttpServletResponse login = login();

        String code = "NOTIFY_FLOW_" + System.nanoTime();
        MvcResult def = mockMvc.perform(post("/api/v1/workflows/definitions")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"code":"%s","name":"Notify flow","graphJson":%s}
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

        mockMvc.perform(get("/api/v1/notifications").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/notifications/unread-count").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", greaterThanOrEqualTo(1)));

        MvcResult list = mockMvc.perform(get("/api/v1/notifications").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode first = objectMapper.readTree(list.getResponse().getContentAsString()).get("content").get(0);
        String notificationId = first.get("id").asText();

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/notification-preferences")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"channel\":\"IN_APP\",\"eventType\":\"TaskCreated\",\"enabled\":true}]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").value("TaskCreated"));

        mockMvc.perform(get("/api/v1/notification-preferences").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
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
