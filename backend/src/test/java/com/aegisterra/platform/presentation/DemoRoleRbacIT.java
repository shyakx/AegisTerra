package com.aegisterra.platform.presentation;

import com.aegisterra.platform.support.SharedPostgresContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 025 — focused demo-persona RBAC matrix against live seeded accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoRoleRbacIT extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedFarmersIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/farmers")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({
        "admin,Admin@1234!Aa,SYSTEM_ADMIN",
        "insurance.admin,Demo@1234!Aa,INSURANCE_ADMIN",
        "insurance.officer,Demo@1234!Aa,INSURANCE_OFFICER",
        "fi.officer,Demo@1234!Aa,FI_OFFICER",
        "gov.analyst,Demo@1234!Aa,GOVERNMENT_ANALYST",
        "aggregator,Demo@1234!Aa,AGGREGATOR",
        "farmer.demo,Demo@1234!Aa,FARMER",
        "auditor,Demo@1234!Aa,AUDITOR",
        "support,Demo@1234!Aa,SUPPORT"
    })
    void demoAccountsLoginWithExpectedRole(String username, String password, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.roles", hasItem(role)));
    }

    @Test
    void adminCanAccessUsersAndSettlements() throws Exception {
        Cookie at = loginCookie("admin", "Admin@1234!Aa");
        mockMvc.perform(get("/api/v1/users").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/settlements").cookie(at)).andExpect(status().isOk());
    }

    @Test
    void insuranceOfficerCanReadClaimsButNotUsersOrSettlements() throws Exception {
        Cookie at = loginCookie("insurance.officer", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/claims").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/policies").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").cookie(at)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/settlements").cookie(at)).andExpect(status().isForbidden());
    }

    @Test
    void fiOfficerCanReadSettlementsButNotUsersOrClaimsWriteSurface() throws Exception {
        Cookie at = loginCookie("fi.officer", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/settlements").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ledger").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").cookie(at)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/auth/me").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", not(hasItem("claims:write"))));
    }

    @Test
    void governmentAnalystHasClimateReadsAndNoUsersWrite() throws Exception {
        Cookie at = loginCookie("gov.analyst", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/climate-intel/national/dashboard").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/executive/overview").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").cookie(at)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/auth/me").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", not(hasItem("users:write"))))
            .andExpect(jsonPath("$.permissions", not(hasItem("settlements:process"))));
    }

    @Test
    void farmerCanAccessOwnRegistryButNotUsersOrSettlements() throws Exception {
        Cookie at = loginCookie("farmer.demo", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/farmers").cookie(at)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].farmerCode").value("FRM-2026-001"));
        mockMvc.perform(get("/api/v1/farms").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/policies").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/claims").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users").cookie(at)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/settlements").cookie(at)).andExpect(status().isForbidden());
    }

    @Test
    void auditorCanReadClaimsAndSettlementsButNotMutateUsers() throws Exception {
        Cookie at = loginCookie("auditor", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/claims").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/settlements").cookie(at)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions", not(hasItem("claims:write"))))
            .andExpect(jsonPath("$.permissions", not(hasItem("settlements:process"))))
            .andExpect(jsonPath("$.permissions", not(hasItem("users:write"))));
    }

    @Test
    void insuranceOfficerSeesClaimTaskInMyInbox() throws Exception {
        Cookie at = loginCookie("insurance.officer", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/tasks/my").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[*].subjectType", hasItem("CLAIM")))
            .andExpect(jsonPath("$.content[*].assigneeRoleCode", hasItem("INSURANCE_OFFICER")));
    }

    @Test
    void fiOfficerSeesSettlementTaskInMyInbox() throws Exception {
        Cookie at = loginCookie("fi.officer", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/tasks/my").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[*].subjectType", hasItem("SETTLEMENT")))
            .andExpect(jsonPath("$.content[*].assigneeRoleCode", hasItem("FI_OFFICER")));
    }

    @ParameterizedTest
    @CsvSource({
        "farmer.demo,Demo@1234!Aa",
        "insurance.officer,Demo@1234!Aa",
        "fi.officer,Demo@1234!Aa"
    })
    void demoPersonasReceiveSeededNotifications(String username, String password) throws Exception {
        Cookie at = loginCookie(username, password);
        mockMvc.perform(get("/api/v1/notifications").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    void farmerCannotAccessTaskInbox() throws Exception {
        Cookie at = loginCookie("farmer.demo", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/tasks/my").cookie(at)).andExpect(status().isForbidden());
    }

    @Test
    void systemAdminStillSeesOpenTaskInMyInbox() throws Exception {
        Cookie at = loginCookie("admin", "Admin@1234!Aa");
        mockMvc.perform(get("/api/v1/tasks/my").cookie(at))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[*].assigneeRoleCode", hasItem("SYSTEM_ADMIN")));
    }

    private Cookie loginCookie(String username, String password) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(username, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
        return response.getCookie("at");
    }

    private static String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"rememberMe\":false}";
    }
}
