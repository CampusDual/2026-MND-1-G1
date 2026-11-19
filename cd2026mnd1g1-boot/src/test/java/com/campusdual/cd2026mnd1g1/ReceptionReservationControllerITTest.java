package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Import(ReceptionReservationControllerITTest.ArrivalTestClockConfiguration.class)
class ReceptionReservationControllerITTest {

    private static final String CENTER_CIF = "B00000000";
    private static final String CLIENT_CIF = "B00000001";
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 11, 17);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock receptionClock;

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
        this.mockMvc.perform(get("/api/recepcion/reservas/{id}/datos-salida", reservationId)
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("cliente-b2b"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"TUTOR\"}"))
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
    void arrivalPersistsServerPresenceAndObservationsAndRejectsRetry() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-01", "Mochila", today, "CONFIRMADA"
        );
        String observations = "Mochila azul; correa roja; recipiente de comida identificado";

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"observations\":\"" + observations
                                + "\",\"type\":\"SALIDA\",\"username\":\"tutor\","
                                + "\"centerId\":999,\"dateTime\":\"2000-01-01T00:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.type").value("ENTRADA"))
                .andExpect(jsonPath("$.dateTime").value("2026-11-17T11:00:00"))
                .andExpect(jsonPath("$.observations").value(observations))
                .andExpect(jsonPath("$.reservationStatus").value("EN_CURSO"));

        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presence.status").value("EN_EL_CENTRO"))
                .andExpect(jsonPath("$.presence.entryObservations").value(observations))
                .andExpect(jsonPath("$.presence.entryResponsible").isNotEmpty())
                .andExpect(jsonPath("$.dog.allergies.length()").value(0))
                .andExpect(jsonPath("$.dog.medications.length()").value(0));
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'ENTRADA'",
                        Integer.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                this.jdbcTemplate.queryForObject(
                        "SELECT id FROM usuarios WHERE username = 'recepcionista'",
                        Long.class
                ),
                this.jdbcTemplate.queryForObject(
                        "SELECT usuario_id FROM check_registro WHERE reserva_id = ? AND tipo = 'ENTRADA'",
                        Long.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "DIA_COMPLETO",
                this.jdbcTemplate.queryForObject(
                        "SELECT modalidad FROM reserva WHERE id = ?",
                        String.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                today,
                this.jdbcTemplate.queryForObject(
                        "SELECT fecha FROM reserva WHERE id = ?",
                        LocalDate.class,
                        reservationId
                )
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"observations\":\"reintento\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARRIVAL_ALREADY_RECORDED"));

        long noObservationsReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-NOOBS", "Sin notas", today, "CONFIRMADA"
        );
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", noObservationsReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.observations").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void arrivalRejectsMissingSessionIneligibleDatesAndCancelledReservation() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long todayReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-02", "Hoy", today, "CONFIRMADA"
        );
        long futureReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-03", "Futuro", today.plusDays(1), "CONFIRMADA"
        );
        long pastReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-04", "Pasado", today.minusDays(1), "CONFIRMADA"
        );
        long cancelledReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-05", "Cancelada", today, "CANCELADA"
        );
        long foreignReservation = insertReservation(
                createClientCenterAndUser(), "TEST-ARRIVAL-DNI-07", "Ajeno", today, "CONFIRMADA"
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", todayReservation)
                        .contentType("application/json")
                        .content("{\"observations\":null}"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", futureReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", pastReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", cancelledReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isConflict());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", foreignReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", todayReservation)
                        .header("Authorization", basicAuthorization("tutor"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void arrivalRejectsAnExistingExitWithoutRepairingTheInconsistentRecord() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-ARRIVAL-DNI-06", "Incoherente", today, "CONFIRMADA"
        );
        long userId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'SALIDA', ?)",
                reservationId,
                userId,
                LocalDateTime.now(this.receptionClock)
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INCONSISTENT_CHECK_RECORD"));

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'ENTRADA'",
                        Integer.class,
                        reservationId
                )
        );
    }

    @Test
    void departurePersistsCollectorAndOptionalObservationsAndPreservesReservationAndEntry() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-TUTOR", "Salida tutor", today, "CONFIRMADA"
        );
        long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = (SELECT perro_id FROM reserva WHERE id = ?)",
                Long.class,
                reservationId
        );
        long receptionUserId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        LocalDateTime entryAt = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 9, 0);
        this.jdbcTemplate.update("UPDATE reserva SET estado = 'EN_CURSO' WHERE id = ?", reservationId);
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora, observaciones) "
                        + "VALUES (?, ?, 'ENTRADA', ?, 'Bolsa azul y arnés')",
                reservationId,
                receptionUserId,
                entryAt
        );

        this.mockMvc.perform(get("/api/recepcion/reservas/{id}/datos-salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorId").value(tutorId))
                .andExpect(jsonPath("$.presenceStatus").value("EN_EL_CENTRO"))
                .andExpect(jsonPath("$.entryAt").value(entryAt.toString() + ":00"))
                .andExpect(jsonPath("$.authorizedPersons.length()").value(0));

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"TUTOR\",\"authorizedPersonId\":null,"
                                + "\"observations\":\"El tutor volverá mañana\","
                                + "\"userId\":999,\"centerId\":999,\"dateTime\":\"2000-01-01T00:00:00\","
                                + "\"type\":\"ENTRADA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SALIDA"))
                .andExpect(jsonPath("$.dateTime").value("2026-11-17T11:00:00"))
                .andExpect(jsonPath("$.collectorType").value("TUTOR"))
                .andExpect(jsonPath("$.collectorName").value("Tutor TEST-DEP-TUTOR Prueba"))
                .andExpect(jsonPath("$.reservationStatus").value("COMPLETADA"))
                .andExpect(jsonPath("$.observations").value("El tutor volverá mañana"));

        org.junit.jupiter.api.Assertions.assertEquals(
                2,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ?",
                        Integer.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'ENTRADA'",
                        Integer.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "COMPLETADA",
                this.jdbcTemplate.queryForObject(
                        "SELECT estado FROM reserva WHERE id = ?",
                        String.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                receptionUserId,
                this.jdbcTemplate.queryForObject(
                        "SELECT usuario_id FROM check_registro WHERE reserva_id = ? AND tipo = 'SALIDA'",
                        Long.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "El tutor volverá mañana",
                this.jdbcTemplate.queryForObject(
                        "SELECT observaciones FROM check_registro WHERE reserva_id = ? AND tipo = 'SALIDA'",
                        String.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "DIA_COMPLETO",
                this.jdbcTemplate.queryForObject(
                        "SELECT modalidad FROM reserva WHERE id = ?",
                        String.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                today,
                this.jdbcTemplate.queryForObject("SELECT fecha FROM reserva WHERE id = ?", LocalDate.class, reservationId)
        );

        this.mockMvc.perform(get("/api/recepcion/reservas/{id}", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presence.status").value("SALIDA_REGISTRADA"))
                .andExpect(jsonPath("$.presence.exitResponsible").isNotEmpty())
                .andExpect(jsonPath("$.presence.collectorName").value("Tutor TEST-DEP-TUTOR Prueba"))
                .andExpect(jsonPath("$.presence.exitObservations").isNotEmpty());

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"TUTOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_ELIGIBLE"));
    }

    @Test
    void departureAcceptsOnlyAnAuthorizedPersonOfTheReservationTutor() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-AUTHORIZED", "Salida autorizada", today, "EN_CURSO"
        );
        long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = (SELECT perro_id FROM reserva WHERE id = ?)",
                Long.class,
                reservationId
        );
        long userId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        this.jdbcTemplate.update(
                "INSERT INTO persona_autorizada (tutor_id, nombre, telefono) VALUES (?, ?, ?)",
                tutorId,
                "Persona autorizada correcta",
                "600000002"
        );
        long authorizedPersonId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM persona_autorizada WHERE tutor_id = ? AND nombre = ?",
                Long.class,
                tutorId,
                "Persona autorizada correcta"
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'ENTRADA', ?)",
                reservationId,
                userId,
                LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 9, 0)
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"PERSONA_AUTORIZADA\",\"authorizedPersonId\":"
                                + authorizedPersonId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collectorType").value("PERSONA_AUTORIZADA"))
                .andExpect(jsonPath("$.collectorName").value("Persona autorizada correcta"));

        org.junit.jupiter.api.Assertions.assertEquals(
                authorizedPersonId,
                this.jdbcTemplate.queryForObject(
                        "SELECT recogido_por_persona_autorizada_id FROM check_registro "
                                + "WHERE reserva_id = ? AND tipo = 'SALIDA'",
                        Long.class,
                        reservationId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro "
                                + "WHERE reserva_id = ? AND tipo = 'SALIDA' AND recogido_por_tutor_id IS NOT NULL",
                        Integer.class,
                        reservationId
                )
        );
    }

    @Test
    void departureRejectsMissingEntryAndForeignAuthorizedPerson() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long noEntryReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-NO-ENTRY", "Sin entrada", today, "EN_CURSO"
        );
        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", noEntryReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"TUTOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INCONSISTENT_CHECK_RECORD"));

        long firstReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-OWNER-1", "Perro uno", today, "EN_CURSO"
        );
        long secondReservation = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-OWNER-2", "Perro dos", today, "EN_CURSO"
        );
        long userId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        LocalDateTime entryAt = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 9, 0);
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'ENTRADA', ?)",
                firstReservation, userId, entryAt
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'ENTRADA', ?)",
                secondReservation, userId, entryAt
        );
        long foreignTutorId = this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = (SELECT perro_id FROM reserva WHERE id = ?)",
                Long.class,
                secondReservation
        );
        this.jdbcTemplate.update(
                "INSERT INTO persona_autorizada (tutor_id, nombre, telefono) VALUES (?, ?, ?)",
                foreignTutorId,
                "Autorizada ajena",
                "600000001"
        );
        long foreignPersonId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM persona_autorizada WHERE tutor_id = ? AND nombre = ?",
                Long.class,
                foreignTutorId,
                "Autorizada ajena"
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", firstReservation)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"PERSONA_AUTORIZADA\",\"authorizedPersonId\":"
                                + foreignPersonId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COLLECTOR_NOT_AUTHORIZED"));

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'SALIDA'",
                        Integer.class,
                        firstReservation
                )
        );
    }

    @Test
    void departureAllowsClosingAnOlderOpenStayUsingCurrentServerTime() throws Exception {
        LocalDate yesterday = LocalDate.now(this.receptionClock).minusDays(1);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-OLD", "Estancia antigua", yesterday, "EN_CURSO"
        );
        long userId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'ENTRADA', ?)",
                reservationId,
                userId,
                LocalDateTime.of(yesterday.getYear(), yesterday.getMonth(), yesterday.getDayOfMonth(), 9, 0)
        );

        this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"TUTOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateTime").value("2026-11-17T11:00:00"));
        org.junit.jupiter.api.Assertions.assertEquals(
                yesterday,
                this.jdbcTemplate.queryForObject(
                        "SELECT fecha FROM reserva WHERE id = ?",
                        LocalDate.class,
                        reservationId
                )
        );
    }

    @Test
    void legacyGenericCheckRecordEndpointCannotBypassDepartureValidation() throws Exception {
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-DEP-LEGACY", "Salida antigua", LocalDate.now(this.receptionClock), "EN_CURSO"
        );

        this.mockMvc.perform(post("/api/reservas/{id}/check-registros", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"type\":\"SALIDA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTURE_ENDPOINT_REQUIRED"));
        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                this.jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ?",
                        Integer.class,
                        reservationId
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousArrivalsPersistOnlyOneEntry() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        long reservationId = insertReservation(
                centerId(CENTER_CIF), "TEST-ARR-CONC", "Concurrencia", today, "CONFIRMADA"
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> performConcurrentArrival(reservationId, ready, start));
            var second = executor.submit(() -> performConcurrentArrival(reservationId, ready, start));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int firstStatus = first.get(10, TimeUnit.SECONDS);
            int secondStatus = second.get(10, TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertTrue(
                    (firstStatus == 201 && secondStatus == 409)
                            || (firstStatus == 409 && secondStatus == 201),
                    "Respuestas concurrentes inesperadas: " + firstStatus + " y " + secondStatus
            );
            org.junit.jupiter.api.Assertions.assertEquals(
                    1,
                    this.jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'ENTRADA'",
                            Integer.class,
                            reservationId
                    )
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            this.jdbcTemplate.update("DELETE FROM check_registro WHERE reserva_id = ?", reservationId);
            this.jdbcTemplate.update("DELETE FROM reserva WHERE id = ?", reservationId);
            Long dogId = this.jdbcTemplate.queryForObject("""
                    SELECT id FROM perro
                    WHERE nombre = 'Concurrencia'
                      AND tutor_id = (SELECT id FROM tutor WHERE dni = 'TEST-ARR-CONC')
                    """, Long.class);
            this.jdbcTemplate.update("DELETE FROM perro WHERE id = ?", dogId);
            this.jdbcTemplate.update("DELETE FROM tutor WHERE dni = 'TEST-ARR-CONC'");
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousDeparturesPersistOneOutputAndKeepTheWinningCollector() throws Exception {
        LocalDate today = LocalDate.now(this.receptionClock);
        String tutorDni = "TEST-DEP-CONC";
        long reservationId = insertReservation(
                centerId(CENTER_CIF), tutorDni, "Salida concurrencia", today, "EN_CURSO"
        );
        long userId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'", Long.class
        );
        long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT tutor_id FROM perro WHERE id = (SELECT perro_id FROM reserva WHERE id = ?)",
                Long.class,
                reservationId
        );
        this.jdbcTemplate.update(
                "INSERT INTO persona_autorizada (tutor_id, nombre, telefono) VALUES (?, ?, ?)",
                tutorId,
                "Autorizada concurrencia",
                "600000003"
        );
        long authorizedPersonId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM persona_autorizada WHERE tutor_id = ? AND nombre = ?",
                Long.class,
                tutorId,
                "Autorizada concurrencia"
        );
        this.jdbcTemplate.update(
                "INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora) VALUES (?, ?, 'ENTRADA', ?)",
                reservationId,
                userId,
                LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 9, 0)
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() ->
                    performConcurrentDeparture(reservationId, ready, start, "TUTOR", null));
            var second = executor.submit(() ->
                    performConcurrentDeparture(reservationId, ready, start, "PERSONA_AUTORIZADA", authorizedPersonId));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int firstStatus = first.get(10, TimeUnit.SECONDS);
            int secondStatus = second.get(10, TimeUnit.SECONDS);
            org.junit.jupiter.api.Assertions.assertTrue(
                    (firstStatus == 201 && secondStatus == 409)
                            || (firstStatus == 409 && secondStatus == 201),
                    "Respuestas concurrentes inesperadas: " + firstStatus + " y " + secondStatus
            );
            org.junit.jupiter.api.Assertions.assertEquals(
                    1,
                    this.jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM check_registro WHERE reserva_id = ? AND tipo = 'SALIDA'",
                            Integer.class,
                            reservationId
                    )
            );
            Long storedTutorId = this.jdbcTemplate.queryForObject(
                    "SELECT recogido_por_tutor_id FROM check_registro WHERE reserva_id = ? AND tipo = 'SALIDA'",
                    Long.class,
                    reservationId
            );
            Long storedAuthorizedId = this.jdbcTemplate.queryForObject(
                    "SELECT recogido_por_persona_autorizada_id FROM check_registro "
                            + "WHERE reserva_id = ? AND tipo = 'SALIDA'",
                    Long.class,
                    reservationId
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                    firstStatus == 201
                            ? storedTutorId != null && storedTutorId == tutorId && storedAuthorizedId == null
                            : storedAuthorizedId != null && storedAuthorizedId.equals(authorizedPersonId)
                                    && storedTutorId == null,
                    "El destinatario persistido debe pertenecer a la única petición ganadora"
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            this.jdbcTemplate.update("DELETE FROM check_registro WHERE reserva_id = ?", reservationId);
            this.jdbcTemplate.update("DELETE FROM reserva WHERE id = ?", reservationId);
            this.jdbcTemplate.update("DELETE FROM persona_autorizada WHERE id = ?", authorizedPersonId);
            Long dogId = this.jdbcTemplate.queryForObject("""
                    SELECT id FROM perro
                    WHERE nombre = 'Salida concurrencia'
                      AND tutor_id = (SELECT id FROM tutor WHERE dni = ?)
                    """, Long.class, tutorDni);
            this.jdbcTemplate.update("DELETE FROM perro WHERE id = ?", dogId);
            this.jdbcTemplate.update("DELETE FROM tutor WHERE dni = ?", tutorDni);
        }
    }

    private int performConcurrentArrival(
            long reservationId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se liberó la barrera de solicitudes concurrentes");
        }
        return this.mockMvc.perform(post("/api/recepcion/reservas/{id}/entrada", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"observations\":\"bolsa de prueba\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int performConcurrentDeparture(
            long reservationId,
            CountDownLatch ready,
            CountDownLatch start,
            String collectorType,
            Long authorizedPersonId
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se liberó la barrera de solicitudes concurrentes");
        }
        String personReference = authorizedPersonId == null
                ? ""
                : ",\"authorizedPersonId\":" + authorizedPersonId;
        return this.mockMvc.perform(post("/api/recepcion/reservas/{id}/salida", reservationId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType("application/json")
                        .content("{\"collectorType\":\"" + collectorType + "\"" + personReference + "}"))
                .andReturn()
                .getResponse()
                .getStatus();
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

    @TestConfiguration(proxyBeanMethods = false)
    static class ArrivalTestClockConfiguration {

        @Bean
        @Primary
        Clock arrivalTestClock() {
            return Clock.fixed(
                    LocalDateTime.of(2026, 11, 17, 11, 0).atZone(java.time.ZoneId.of("Europe/Madrid")).toInstant(),
                    java.time.ZoneId.of("Europe/Madrid")
            );
        }
    }
}
