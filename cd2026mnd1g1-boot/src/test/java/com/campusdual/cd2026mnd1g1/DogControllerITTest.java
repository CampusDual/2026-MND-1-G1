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
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class DogControllerITTest {

    private static final String CENTER_CIF = "B00000000";
    private static final String TEST_DNI = "DOG-IT-TUTOR-01";
    private static final String DOG_PATH = "/api/recepcion/perros";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dogEndpointsRequireAuthenticationAndRejectTutorRole() throws Exception {
        this.mockMvc.perform(get(DOG_PATH))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get(DOG_PATH)
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("tutor"))
                        .contentType("application/json")
                        .content(createRequest(0L, "Luna", false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUsesAuthenticatedCenterAndAnExistingTutor() throws Exception {
        Long tutorId = createTutor(TEST_DNI, "Ana", "García");

        MvcResult createdResult = this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(createRequest(tutorId, "Bóreas", false)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Bóreas"))
                .andExpect(jsonPath("$.tutorId").value(tutorId))
                .andExpect(jsonPath("$.tutorName").value("Ana García"))
                .andReturn();

        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        Long dogCenterId = this.jdbcTemplate.queryForObject(
                "SELECT centro_id FROM perro WHERE nombre = 'Bóreas'", Long.class
        );
        Long dogTutorId = this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE nombre = 'Bóreas'", Long.class
        );
        assertNotNull(createdResult.getResponse().getHeader("Location"));
        assertEquals(centerId, dogCenterId);
        assertEquals(tutorId, dogTutorId);
    }

    @Test
    void listSearchesByDogOrTutorAndDetailsAreCenterScoped() throws Exception {
        Long tutorId = createTutor(TEST_DNI, "Iñaki", "Muñoz");
        createDog(tutorId, "Nube", "PEQUENO");

        this.mockMvc.perform(get(DOG_PATH)
                        .queryParam("search", "Iñaki")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Nube"))
                .andExpect(jsonPath("$.items[0].tutorNationalId").value(TEST_DNI));

        Long dogId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE nombre = 'Nube'", Long.class
        );
        this.mockMvc.perform(get(DOG_PATH + "/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.size").value("PEQUENO"));

        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM centro WHERE cif = 'B00000001'", Integer.class
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                    VALUES ('Cliente integración', 'CLIENTE', 'B00000001', 'Dirección de prueba', TRUE)
                    """);
        }
        Long foreignCenterId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = 'B00000001'", Long.class
        );
        this.jdbcTemplate.update("""
                INSERT INTO perro (centro_id, nombre, tamano, activo)
                VALUES (?, 'Perro ajeno', 'MEDIANO', TRUE)
                """, foreignCenterId);
        Long foreignDogId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE nombre = 'Perro ajeno'", Long.class
        );
        this.mockMvc.perform(get(DOG_PATH + "/{id}", foreignDogId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateKeepsTutorAndReservationAndRejectsStaleEtag() throws Exception {
        Long tutorId = createTutor(TEST_DNI, "Ana", "García");
        Long dogId = createDog(tutorId, "Nube", "MEDIANO");
        this.jdbcTemplate.update(
                "UPDATE perro SET raza = 'Caniche', fecha_nacimiento = DATE '2020-01-01' WHERE id = ?",
                dogId
        );
        this.jdbcTemplate.update("""
                INSERT INTO reserva (perro_id, fecha, modalidad, estado)
                VALUES (?, ?, 'DIA_COMPLETO', 'CONFIRMADA')
                """, dogId, LocalDate.now().plusDays(1));

        MvcResult detail = this.mockMvc.perform(get(DOG_PATH + "/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andReturn();
        String etag = detail.getResponse().getHeader("ETag");
        assertNotNull(etag);

        String updateRequest = """
                {
                  "name":"Nube actualizada",
                  "size":"GRANDE",
                  "confirmMatches":false,
                  "tutorId":999999,
                  "centerId":999999,
                  "active":false
                }
                """;
        MvcResult updatedResult = this.mockMvc.perform(put(DOG_PATH + "/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .header("If-Match", etag)
                        .contentType("application/json")
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(dogId))
                .andExpect(jsonPath("$.name").value("Nube actualizada"))
                .andExpect(jsonPath("$.breed").value("Caniche"))
                .andExpect(jsonPath("$.birthDate").value("2020-01-01"))
                .andExpect(jsonPath("$.tutorId").value(tutorId))
                .andReturn();

        assertEquals(tutorId, this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = ?", Long.class, dogId
        ));
        assertEquals(Boolean.TRUE, this.jdbcTemplate.queryForObject(
                "SELECT activo FROM perro WHERE id = ?", Boolean.class, dogId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reserva WHERE perro_id = ?", Integer.class, dogId
        ));

        String updatedEtag = updatedResult.getResponse().getHeader("ETag");
        assertNotNull(updatedEtag);
        this.mockMvc.perform(put(DOG_PATH + "/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .header("If-Match", updatedEtag)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Nube actualizada",
                                  "size":"GRANDE",
                                  "clearBreed":true,
                                  "clearBirthDate":true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breed").doesNotExist())
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andExpect(jsonPath("$.tutorId").value(tutorId));

        assertEquals(tutorId, this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = ?", Long.class, dogId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE id = ? AND activo = TRUE AND centro_id = "
                        + "(SELECT centro_id FROM tutor WHERE id = ?)",
                Integer.class,
                dogId,
                tutorId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reserva WHERE perro_id = ?", Integer.class, dogId
        ));
        this.mockMvc.perform(put(DOG_PATH + "/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .header("If-Match", etag)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Nube antigua",
                                  "size":"GRANDE",
                                  "confirmMatches":false
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void sameNameAndTutorRequiresConfirmationAndFutureBirthDateIsRejected() throws Exception {
        Long tutorId = createTutor(TEST_DNI, "Ana", "García");
        createDog(tutorId, "Luna", "MEDIANO");

        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(createRequest(tutorId, "Luna", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un perro con el mismo nombre para ese tutor. Confirma si quieres continuar."
                ));
        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(createRequest(tutorId, "Luna", true)))
                .andExpect(status().isCreated());

        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "tutorId":%d,
                                  "name":"Futuro",
                                  "size":"MEDIANO",
                                  "birthDate":"2999-01-01",
                                  "confirmMatches":false
                                }
                                """.formatted(tutorId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foreignTutorAndInvalidSizeAreRejected() throws Exception {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM centro WHERE cif = 'B00000001'", Integer.class
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                    VALUES ('Cliente integración', 'CLIENTE', 'B00000001', 'Dirección de prueba', TRUE)
                    """);
        }
        Long foreignCenterId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = 'B00000001'", Long.class
        );
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor ajeno', 'Prueba', 'DOG-IT-FOREIGN', 'dog-foreign@example.com', '5550199')
                """, foreignCenterId);
        Long foreignTutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE dni = 'DOG-IT-FOREIGN'", Long.class
        );
        assertNotNull(centerId);
        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content(createRequest(foreignTutorId, "Luna", false)))
                .andExpect(status().isNotFound());

        Long localTutorId = createTutor(TEST_DNI, "Ana", "García");
        this.mockMvc.perform(post(DOG_PATH)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "tutorId":%d,
                                  "name":"Luna",
                                  "size":"GIGANTE",
                                  "confirmMatches":false
                                }
                                """.formatted(localTutorId)))
                .andExpect(status().isBadRequest());
    }

    private Long createTutor(String dni, String firstName, String lastName) {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, ?, ?, ?, ?, '5550101')
                """, centerId, firstName, lastName, dni, dni.toLowerCase() + "@example.com");
        return tutorId(dni);
    }

    private Long tutorId(String dni) {
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE dni = ?", Long.class, dni
        );
    }

    private Long createDog(Long tutorId, String name, String size) {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        this.jdbcTemplate.update("""
                INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
                VALUES (?, ?, ?, ?, TRUE)
                """, centerId, tutorId, name, size);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE tutor_id = ? AND nombre = ?", Long.class, tutorId, name
        );
    }

    private String createRequest(Long tutorId, String name, boolean confirmMatches) {
        return """
                {
                  "tutorId":%d,
                  "name":"%s",
                  "breed":"",
                  "size":"MEDIANO",
                  "birthDate":null,
                  "confirmMatches":%s
                }
                """.formatted(tutorId, name, confirmMatches);
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }
}
