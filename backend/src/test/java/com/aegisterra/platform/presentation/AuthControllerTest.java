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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSetsHttpOnlyCookiesAndReturnsUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin@1234!Aa\",\"rememberMe\":false}"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("at"))
            .andExpect(cookie().httpOnly("at", true))
            .andExpect(cookie().exists("rt"))
            .andExpect(jsonPath("$.user.username").value("admin"))
            .andExpect(jsonPath("$.user.roles", hasItem("SYSTEM_ADMIN")))
            .andExpect(jsonPath("$.user.permissions", hasItem("users:read")));
    }

    @Test
    void loginRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meAndRefreshWorkWithCookies() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin@1234!Aa\"}"))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpServletResponse loginResponse = login.getResponse();
        String access = loginResponse.getCookie("at").getValue();
        String refresh = loginResponse.getCookie("rt").getValue();
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me").cookie(loginResponse.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(loginResponse.getCookie("rt")))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("at"))
            .andExpect(cookie().exists("rt"))
            .andReturn();

        assertThat(refreshed.getResponse().getCookie("rt").getValue()).isNotEqualTo(refresh);

        mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(refreshed.getResponse().getCookie("at"), refreshed.getResponse().getCookie("rt")))
            .andExpect(status().isOk());
    }
}
