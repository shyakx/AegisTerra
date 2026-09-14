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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GeographyCatalogApiIT extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void geographyEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/provinces")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/districts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/agroecological-zones")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/agroecological-subzones")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsAndGetsReferenceCatalogs() throws Exception {
        MockHttpServletResponse login = login("admin", "Admin@1234!Aa");

        mockMvc.perform(get("/api/v1/provinces").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[*].code", contains("EAST", "KIGALI", "NORTH", "SOUTH", "WEST")));

        mockMvc.perform(get("/api/v1/districts").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(30)));

        mockMvc.perform(get("/api/v1/agroecological-zones").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[*].code", contains("A", "B", "C", "D", "E")));

        mockMvc.perform(get("/api/v1/agroecological-subzones").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(18)));

        JsonNode provinces = read(login, "/api/v1/provinces");
        String northId = findIdByCode(provinces, "NORTH");
        mockMvc.perform(get("/api/v1/provinces/" + northId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Northern Province"));

        JsonNode districts = read(login, "/api/v1/districts");
        String musanzeId = findIdByCode(districts, "MUSANZE");
        mockMvc.perform(get("/api/v1/districts/" + musanzeId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Musanze"))
            .andExpect(jsonPath("$.provinceCode").value("NORTH"))
            .andExpect(jsonPath("$.agroecologicalSubzoneCode").value("A1"))
            .andExpect(jsonPath("$.agroecologicalZoneCode").value("A"));

        JsonNode zones = read(login, "/api/v1/agroecological-zones");
        String zoneAId = findIdByCode(zones, "A");
        mockMvc.perform(get("/api/v1/agroecological-zones/" + zoneAId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("ZONE A"));

        JsonNode subzones = read(login, "/api/v1/agroecological-subzones");
        String a1Id = findIdByCode(subzones, "A1");
        mockMvc.perform(get("/api/v1/agroecological-subzones/" + a1Id).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Volcanic Highlands"))
            .andExpect(jsonPath("$.zoneCode").value("A"));
    }

    @Test
    void filtersDistrictsByProvinceAndSubzonesByZoneOrDistrict() throws Exception {
        MockHttpServletResponse login = login("admin", "Admin@1234!Aa");
        String northId = findIdByCode(read(login, "/api/v1/provinces"), "NORTH");
        String zoneAId = findIdByCode(read(login, "/api/v1/agroecological-zones"), "A");
        String musanzeId = findIdByCode(read(login, "/api/v1/districts"), "MUSANZE");

        mockMvc.perform(get("/api/v1/districts").param("provinceId", northId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[*].code", contains("BURERA", "GAKENKE", "GICUMBI", "MUSANZE", "RULINDO")));

        mockMvc.perform(get("/api/v1/agroecological-subzones").param("zoneId", zoneAId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[*].code", contains("A1", "A2", "A3")));

        mockMvc.perform(get("/api/v1/agroecological-subzones").param("districtId", musanzeId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].code").value("A1"));
    }

    @Test
    void farmerWithReadPermissionCanListCatalogsAndUnknownIdsAreNotFound() throws Exception {
        MockHttpServletResponse login = login("farmer.demo", "Demo@1234!Aa");
        mockMvc.perform(get("/api/v1/districts").cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(30)));

        mockMvc.perform(get("/api/v1/provinces/" + UUID.randomUUID()).cookie(login.getCookie("at")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/districts/" + UUID.randomUUID()).cookie(login.getCookie("at")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/agroecological-zones/" + UUID.randomUUID()).cookie(login.getCookie("at")))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/agroecological-subzones/" + UUID.randomUUID()).cookie(login.getCookie("at")))
            .andExpect(status().isNotFound());
    }

    private JsonNode read(MockHttpServletResponse login, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String findIdByCode(JsonNode array, String code) {
        for (JsonNode node : array) {
            if (code.equals(node.get("code").asText())) {
                return node.get("id").asText();
            }
        }
        throw new IllegalStateException("Missing catalog code " + code);
    }

    private MockHttpServletResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse();
    }
}
