package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class TutorControllerITTest {

    private static final String CENTER_CIF = "B00000000";
    private static final String TEST_DNI = "TEST-TUTOR-IT-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tutorEndpointsRequireAuthenticationAndRejectTutorRole() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/tutores"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("tutor"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "persona@example.com", false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUsesAuthenticatedCenterAndDoesNotCreateUser() throws Exception {
        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName":"María del Mar",
                                  "lastName":"O'Neill",
                                  "nationalId":"%s",
                                  "phone":"+1 202 555 0101",
                                  "email":"persona@example.com",
                                  "confirmMatches":false
                                }
                                """.formatted(TEST_DNI)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.firstName").value("María del Mar"))
                .andExpect(jsonPath("$.lastName").value("O'Neill"));

        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        Long tutorCenterId = this.jdbcTemplate.queryForObject(
                "SELECT centro_id FROM tutor WHERE dni = ?", Long.class, TEST_DNI
        );
        Integer userCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE email = ?", Integer.class, "persona@example.com"
        );
        assertNotNull(centerId);
        assertEquals(centerId, tutorCenterId);
        assertEquals(0, userCount);
    }

    @Test
    void matchingContactRequiresConfirmationAndCanThenProceed() throws Exception {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor existente', 'Prueba', 'TEST-TUTOR-MATCH', 'compartido@example.com', '5550100')
                """, centerId);

        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "COMPARTIDO@example.com", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Hay otro tutor con el mismo teléfono o email. Confirma si deseas continuar."
                ));

        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "COMPARTIDO@example.com", true)))
                .andExpect(status().isCreated());
    }

    @Test
    void listCanSearchAndDetailAndEditKeepTutorId() throws Exception {
        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "persona@example.com", false)))
                .andExpect(status().isCreated());

        this.mockMvc.perform(get("/api/recepcion/tutores")
                        .queryParam("search", "persona@example.com")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].nationalId").value(TEST_DNI));

        Long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE dni = ?", Long.class, TEST_DNI
        );
        MvcResult detail = this.mockMvc.perform(get("/api/recepcion/tutores/{id}", tutorId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(tutorId))
                .andReturn();
        String etag = detail.getResponse().getHeader("ETag");
        assertNotNull(etag);

        this.mockMvc.perform(put("/api/recepcion/tutores/{id}", tutorId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .header("If-Match", etag)
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName":"María",
                                  "lastName":"O'Neill actualizada",
                                  "nationalId":"%s",
                                  "phone":"+1 202 555 0102",
                                  "email":"actualizada@example.com",
                                  "confirmMatches":false
                                }
                                """.formatted(TEST_DNI)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(tutorId))
                .andExpect(jsonPath("$.lastName").value("O'Neill actualizada"));

        this.mockMvc.perform(put("/api/recepcion/tutores/{id}", tutorId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .header("If-Match", etag)
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "stale@example.com", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidFieldsAndDuplicateNationalIdAreRejected() throws Exception {
        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName":"   ",
                                  "lastName":"Apellidos",
                                  "nationalId":"%s",
                                  "phone":"5550101",
                                  "email":"invalido",
                                  "confirmMatches":false
                                }
                                """.formatted(TEST_DNI)))
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "persona@example.com", false)))
                .andExpect(status().isCreated());
        this.mockMvc.perform(post("/api/recepcion/tutores")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(validRequest(TEST_DNI, "otra@example.com", false)))
                .andExpect(status().isConflict());
    }

    @Test
    void tutorFromAnotherCenterIsNotFound() throws Exception {
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM centro WHERE cif = 'B00000001'", Integer.class
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                    VALUES ('Cliente integración', 'CLIENTE', 'B00000001', 'Dirección de prueba', TRUE)
                    """);
        }
        Long foreignCenterId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = 'B00000001'",
                Long.class
        );
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor ajeno', 'Prueba', 'TEST-TUTOR-FOREIGN', 'ajeno@example.com', '5550199')
                """, foreignCenterId);
        Long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE dni = 'TEST-TUTOR-FOREIGN'", Long.class
        );

        this.mockMvc.perform(get("/api/recepcion/tutores/{id}", tutorId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    private String validRequest(String dni, String email, boolean confirmMatches) {
        return """
                {
                  "firstName":"Ana",
                  "lastName":"García López",
                  "nationalId":"%s",
                  "phone":"5550101",
                  "email":"%s",
                  "confirmMatches":%s
                }
                """.formatted(dni, email, confirmMatches);
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }
}
