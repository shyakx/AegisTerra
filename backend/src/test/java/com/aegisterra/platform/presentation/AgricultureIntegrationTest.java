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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgricultureIntegrationTest extends SharedPostgresContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationDraftValidateBoundaryAndSubmit() throws Exception {
        MockHttpServletResponse login = login();
        long n = System.nanoTime();
        String nationalId = "1197" + String.format("%012d", n % 1_000_000_000_000L);
        String phone = "072" + String.format("%07d", n % 10_000_000);
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[30.05,-1.95],[30.051,-1.95],[30.051,-1.949],[30.05,-1.949],[30.05,-1.95]]]}";

        mockMvc.perform(post("/api/v1/farm-boundaries/validate")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"geoJson\":" + objectMapper.writeValueAsString(geoJson) + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));

        String payload = """
            {
              "household":{"code":"HH-%d","headName":"Head"},
              "farmer":{"firstName":"Paul","lastName":"Nkurunziza","nationalId":"%s","phoneNumber":"%s"},
              "farm":{"farmName":"Valley Farm %d","farmSizeHa":1.2},
              "boundary":{"geoJson":%s},
              "plots":[{"plotCode":"P1","name":"Plot 1"}],
              "cropSeasons":[]
            }
            """.formatted(n, nationalId, phone, n, objectMapper.writeValueAsString(geoJson));

        MvcResult draftResult = mockMvc.perform(post("/api/v1/registration-drafts")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentStep\":8,\"payloadJson\":" + objectMapper.writeValueAsString(payload) + "}"))
            .andExpect(status().isCreated())
            .andReturn();

        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult submit = mockMvc.perform(post("/api/v1/registration-drafts/" + draftId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.farmerId").isNotEmpty())
            .andExpect(jsonPath("$.farmId").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(submit.getResponse().getContentAsString());
        mockMvc.perform(get("/api/v1/farmers/" + body.get("farmerId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Paul"));
        mockMvc.perform(get("/api/v1/farm-boundaries").param("farmId", body.get("farmId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void registrationPersistsFarmDistrictAndCopiesToFarmerWhenResidenceMissing() throws Exception {
        MockHttpServletResponse login = login();
        String musanzeId = musanzeId();
        JsonNode submit = submitRegistration(login, musanzeId, null);

        mockMvc.perform(get("/api/v1/farmers/" + submit.get("farmerId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districtId").value(musanzeId));
        mockMvc.perform(get("/api/v1/farms/" + submit.get("farmId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districtId").value(musanzeId));

        mockMvc.perform(get("/api/v1/districts/" + musanzeId).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provinceCode").value("NORTH"))
            .andExpect(jsonPath("$.agroecologicalZoneCode").value("A"))
            .andExpect(jsonPath("$.agroecologicalSubzoneCode").value("A1"));
    }

    @Test
    void registrationPreservesExplicitFarmerResidenceWhenFarmDistrictDiffers() throws Exception {
        MockHttpServletResponse login = login();
        String musanzeId = musanzeId();
        String gasaboId = jdbcTemplate.queryForObject(
            "SELECT id::text FROM districts WHERE code = 'GASABO' AND deleted = false",
            String.class
        );
        JsonNode submit = submitRegistration(login, musanzeId, gasaboId);

        mockMvc.perform(get("/api/v1/farmers/" + submit.get("farmerId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districtId").value(gasaboId));
        mockMvc.perform(get("/api/v1/farms/" + submit.get("farmId").asText()).cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.districtId").value(musanzeId));
    }

    @Test
    void registrationRejectsUnknownDistrictWithoutCreatingRecords() throws Exception {
        MockHttpServletResponse login = login();
        long farmersBefore = countActive("farmers");
        long farmsBefore = countActive("farms");
        String unknown = UUID.randomUUID().toString();

        long n = System.nanoTime();
        String payload = registrationPayload(n, unknown, unknown);
        MvcResult draftResult = mockMvc.perform(post("/api/v1/registration-drafts")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentStep\":8,\"payloadJson\":" + objectMapper.writeValueAsString(payload) + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/registration-drafts/" + draftId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isBadRequest());

        assertThat(countActive("farmers")).isEqualTo(farmersBefore);
        assertThat(countActive("farms")).isEqualTo(farmsBefore);
    }

    @Test
    void registrationRejectsDeletedDistrict() throws Exception {
        MockHttpServletResponse login = login();
        UUID deletedId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO districts (
                id, code, name, province_id, agroecological_subzone_id,
                created_at, updated_at, version, status, deleted
            )
            SELECT ?, ?, 'Deleted district', province_id, agroecological_subzone_id,
                   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'DISABLED', TRUE
            FROM districts WHERE code = 'MUSANZE' AND deleted = false
            """,
            deletedId,
            "DEL-" + deletedId.toString().substring(0, 8).toUpperCase()
        );

        long n = System.nanoTime();
        String payload = registrationPayload(n, deletedId.toString(), deletedId.toString());
        MvcResult draftResult = mockMvc.perform(post("/api/v1/registration-drafts")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentStep\":8,\"payloadJson\":" + objectMapper.writeValueAsString(payload) + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/registration-drafts/" + draftId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isBadRequest());
    }

    private JsonNode submitRegistration(MockHttpServletResponse login, String farmDistrictId, String farmerDistrictId)
        throws Exception {
        long n = System.nanoTime();
        String payload = registrationPayload(n, farmDistrictId, farmerDistrictId);
        MvcResult draftResult = mockMvc.perform(post("/api/v1/registration-drafts")
                .cookie(login.getCookie("at"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentStep\":8,\"payloadJson\":" + objectMapper.writeValueAsString(payload) + "}"))
            .andExpect(status().isCreated())
            .andReturn();
        String draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).get("id").asText();
        MvcResult submit = mockMvc.perform(post("/api/v1/registration-drafts/" + draftId + "/submit")
                .cookie(login.getCookie("at")))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(submit.getResponse().getContentAsString());
    }

    private String registrationPayload(long n, String farmDistrictId, String farmerDistrictId) throws Exception {
        String nationalId = "1197" + String.format("%012d", n % 1_000_000_000_000L);
        String phone = "072" + String.format("%07d", n % 10_000_000);
        String geoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[30.05,-1.95],[30.051,-1.95],[30.051,-1.949],[30.05,-1.949],[30.05,-1.95]]]}";
        String farmerDistrictJson = farmerDistrictId == null ? "" : ",\"districtId\":\"%s\"".formatted(farmerDistrictId);
        String farmDistrictJson = farmDistrictId == null ? "" : ",\"districtId\":\"%s\"".formatted(farmDistrictId);
        return """
            {
              "household":{"code":"HH-%d","headName":"Head"},
              "farmer":{"firstName":"Paul","lastName":"Nkurunziza","nationalId":"%s","phoneNumber":"%s"%s},
              "farm":{"farmName":"Valley Farm %d","farmSizeHa":1.2%s},
              "boundary":{"geoJson":%s},
              "plots":[{"plotCode":"P1","name":"Plot 1"}],
              "cropSeasons":[]
            }
            """.formatted(n, nationalId, phone, farmerDistrictJson, n, farmDistrictJson, objectMapper.writeValueAsString(geoJson));
    }

    private String musanzeId() {
        return jdbcTemplate.queryForObject(
            "SELECT id::text FROM districts WHERE code = 'MUSANZE' AND deleted = false",
            String.class
        );
    }

    private long countActive(String table) {
        Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + table + " WHERE deleted = false", Long.class);
        return count == null ? 0 : count;
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
