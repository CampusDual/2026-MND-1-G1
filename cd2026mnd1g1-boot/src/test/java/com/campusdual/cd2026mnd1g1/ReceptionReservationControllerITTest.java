package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReceptionReservationControllerITTest {

    private static final String CENTER_CIF = "B00000000";
    private static final String CLIENT_CIF = "B00000001";
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 11, 17);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void receptionistSeesOnlyItsCenterAndRequestedDateInStableOrder() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createClientCenterAndUser();
        insertReservation(ownCenterId, "TEST-R-DNI-01", "Luna", BASE_DATE, "CONFIRMADA");
        insertReservation(ownCenterId, "TEST-R-DNI-02", "Bruno", BASE_DATE, "CONFIRMADA");
        insertReservation(ownCenterId, "TEST-R-DNI-03", "Luna", BASE_DATE, "CONFIRMADA");
        insertReservation(ownCenterId, "TEST-R-DNI-04", "Nala", BASE_DATE, "CANCELADA");
        insertReservation(ownCenterId, "TEST-R-DNI-05", "Kira", BASE_DATE.minusDays(1), "CONFIRMADA");
        insertReservation(clientCenterId, "TEST-B-DNI-01", "Fuera", BASE_DATE, "CONFIRMADA");

        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .queryParam("date", BASE_DATE.toString())
                        .queryParam("centerId", Long.toString(clientCenterId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[0].dogName").value("Bruno"))
                .andExpect(jsonPath("$.items[1].dogName").value("Luna"))
                .andExpect(jsonPath("$.items[2].dogName").value("Luna"))
                .andExpect(jsonPath("$.items[1].dogId").value(dogId("TEST-R-DNI-01")))
                .andExpect(jsonPath("$.items[2].dogId").value(dogId("TEST-R-DNI-03")))
                .andExpect(jsonPath("$.items[3].status").value("CANCELADA"));
    }

    @Test
    void emptyDateReturnsAnEmptyPageAndInvalidDateReturnsBadRequest() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .queryParam("date", "2099-12-30")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));

        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .queryParam("date", "17-11-2026")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailReturnsTutorContactAndPresenceFromRecords() throws Exception {
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-R-DNI-04", "Coco", BASE_DATE, "PENDIENTE"
        );
        long receptionUserId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'",
                Long.class
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, ?, ?)",
                reservationId,
                receptionUserId,
                "SALIDA",
                LocalDateTime.of(2026, 11, 17, 10, 0)
        );

        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dog.name").value("Coco"))
                .andExpect(jsonPath("$.tutor.name").value("Tutor TEST-R-DNI-04 Prueba"))
                .andExpect(jsonPath("$.tutor.phone").value("981000101"))
                .andExpect(jsonPath("$.presence.status").value("REGISTRO_INCOHERENTE"))
                .andExpect(jsonPath("$.presence.inconsistent").value(true))
                .andExpect(jsonPath("$.status").value("PENDIENTE"));
    }

    @Test
    void tutorAndB2bClientCannotUseListOrDetail() throws Exception {
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-R-DNI-05", "Nala", BASE_DATE, "CONFIRMADA"
        );
        createClientCenterAndUser();

        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", reservationId)
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .header("Authorization", basicAuthorization("cliente-b2b")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", reservationId)
                        .header("Authorization", basicAuthorization("cliente-b2b")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAndManagerCanUseReadOnlyQueries() throws Exception {
        insertManagerUser();

        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .header("Authorization", basicAuthorization("admin")))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .header("Authorization", basicAuthorization("gerente")))
                .andExpect(status().isOk());
    }

    @Test
    void foreignReservationAndLegacyListAreNotAvailableToTutor() throws Exception {
        long clientCenterId = createClientCenterAndUser();
        long foreignReservationId = insertReservation(
                clientCenterId, "TEST-B-DNI-02", "Ajeno", BASE_DATE, "CONFIRMADA"
        );

        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", foreignReservationId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/reservas")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void paginationParametersAreValidated() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .queryParam("page", "-1")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());
        this.mockMvc.perform(get("/api/recepcion/reservas")
                        .queryParam("size", "101")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());
    }

    private long createClientCenterAndUser() {
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM centro WHERE cif = ?", Integer.class, CLIENT_CIF
        ) == 0) {
            this.jdbcTemplate.update(
                    "INSERT INTO centro (nombre, tipo, cif, direccion, activo) VALUES (?, ?, ?, ?, TRUE)",
                    "Cliente prueba",
                    "CLIENTE",
                    CLIENT_CIF,
                    "Dirección ficticia"
            );
        }
        long centerId = centerId(CLIENT_CIF);
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE username = 'cliente-b2b'", Integer.class
        ) == 0) {
            long tutorRoleId = this.jdbcTemplate.queryForObject(
                    "SELECT id FROM rol WHERE nombre = 'TUTOR'", Long.class
            );
            String passwordHash = this.jdbcTemplate.queryForObject(
                    "SELECT password_hash FROM usuarios WHERE username = 'tutor'", String.class
            );
            this.jdbcTemplate.update("""
                    INSERT INTO usuarios
                        (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
                    VALUES (?, ?, 'Cliente', 'Prueba', 'TEST-B2B-DNI', 'cliente-test@guarderiademo.test',
                            'cliente-b2b', ?, TRUE)
                    """, centerId, tutorRoleId, passwordHash);
        }
        return centerId;
    }

    private void insertManagerUser() {
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE username = 'gerente'", Integer.class
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO usuarios
                        (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
                    VALUES (?, ?, 'Gerente', 'Prueba', 'TEST-MANAGER-DNI',
                            'gerente-test@guarderiademo.test', 'gerente', ?, TRUE)
                    """,
                    centerId(CENTER_CIF),
                    this.jdbcTemplate.queryForObject(
                            "SELECT id FROM rol WHERE nombre = 'GERENTE'", Long.class
                    ),
                    this.jdbcTemplate.queryForObject(
                            "SELECT password_hash FROM usuarios WHERE username = 'admin'", String.class
                    )
            );
        }
    }

    private long insertReservation(
            long centerId,
            String tutorDni,
            String dogName,
            LocalDate date,
            String status
    ) {
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tutor WHERE centro_id = ? AND dni = ?",
                Integer.class,
                centerId,
                tutorDni
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                    VALUES (?, ?, 'Prueba', ?, ?, '981000101')
                    """,
                    centerId,
                    "Tutor " + tutorDni,
                    tutorDni,
                    tutorDni.toLowerCase() + "@guarderiademo.test"
            );
        }
        long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE centro_id = ? AND dni = ?",
                Long.class,
                centerId,
                tutorDni
        );
        if (this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM perro
                WHERE centro_id = ? AND tutor_id = ? AND nombre = ?
                """, Integer.class, centerId, tutorId, dogName) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
                    VALUES (?, ?, ?, 'MEDIANO', TRUE)
                    """, centerId, tutorId, dogName);
        }
        long dogId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE centro_id = ? AND tutor_id = ? AND nombre = ?",
                Long.class,
                centerId,
                tutorId,
                dogName
        );
        if (this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reserva WHERE perro_id = ? AND fecha = ?",
                Integer.class,
                dogId,
                date
        ) == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO reserva (perro_id, fecha, modalidad, estado)
                    VALUES (?, ?, 'DIA_COMPLETO', ?)
                    """, dogId, date, status);
        }
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM reserva WHERE perro_id = ? AND fecha = ?",
                Long.class,
                dogId,
                date
        );
    }

    private long centerId(String cif) {
        return this.jdbcTemplate.queryForObject("SELECT id FROM centro WHERE cif = ?", Long.class, cif);
    }

    private long dogId(String tutorDni) {
        return this.jdbcTemplate.queryForObject("""
                SELECT dog.id
                FROM perro dog
                JOIN tutor ON tutor.id = dog.tutor_id
                WHERE tutor.centro_id = (SELECT id FROM centro WHERE cif = ?)
                  AND tutor.dni = ?
                """, Long.class, CENTER_CIF, tutorDni);
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }
}
