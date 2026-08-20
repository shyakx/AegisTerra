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
class FarmerControllerTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listFarmersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/farmers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listAndCreateFarmerWithAuth() throws Exception {
        MockHttpServletResponse login = login();

        mockMvc.perform(get("/api/v1/farmers").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(0)));

        String nationalId = "1199" + String.format("%012d", System.nanoTime() % 1_000_000_000_000L);
        String phone = "078" + String.format("%07d", System.nanoTime() % 10_000_000);

        mockMvc.perform(post("/api/v1/farmers")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName":"Jean",
                      "lastName":"Habimana",
                      "nationalId":"%s",
                      "phoneNumber":"%s",
                      "email":"jean.%s@example.com"
                    }
                    """.formatted(nationalId, phone, System.nanoTime())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Jean"))
            .andExpect(jsonPath("$.farmerCode").isNotEmpty());
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
