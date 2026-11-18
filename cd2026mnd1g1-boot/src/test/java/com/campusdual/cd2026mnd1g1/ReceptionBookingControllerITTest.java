package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ReceptionBookingControllerITTest {

    private static final String CENTER_CIF = "B00000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock receptionClock;

    @Test
    void unauthenticatedAndTutorRequestsCannotReadOrCreateBookings() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/grupos"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", nextOpenDate().toString())
                        .queryParam("groupId", "1")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(1, 1, nextOpenDate(), "MANANA"))
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupsAvailabilityAndConfirmedCreationAreScopedAndReturnLocation() throws Exception {
        long groupId = createGroup(3);
        long dogId = createDog(true, CENTER_CIF);
        LocalDate date = nextOpenDate();

        this.mockMvc.perform(get("/api/recepcion/grupos")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", date.toString())
                        .queryParam("groupId", Long.toString(groupId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morningRemaining").value(3))
                .andExpect(jsonPath("$.afternoonRemaining").value(3));

        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(dogId, groupId, date, "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.modality").value("MANANA"))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.groupId").value(groupId));

        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reserva WHERE perro_id = ? AND fecha = ? AND estado = 'CONFIRMADA'",
                Integer.class,
                dogId,
                date
        ));
    }

    @Test
    void dogsAndGroupsFromAnotherCenterCannotBeUsed() throws Exception {
        String foreignCenterCif = createForeignCenter();
        long foreignDogId = createDog(true, foreignCenterCif);
        long foreignGroupId = createGroup(foreignCenterCif, 3);
        LocalDate date = nextOpenDate();

        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", date.toString())
                        .queryParam("groupId", Long.toString(foreignGroupId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(foreignDogId, foreignGroupId, date, "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    @Test
    void morningCapacityDoesNotConsumeAfternoonAndFullDayNeedsBoth() throws Exception {
        long groupId = createGroup(3);
        LocalDate date = nextOpenDate();
        insertReservations(groupId, date, "MANANA", "CONFIRMADA", 3);

        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", date.toString())
                        .queryParam("groupId", Long.toString(groupId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morningOccupied").value(3))
                .andExpect(jsonPath("$.morningRemaining").value(0))
                .andExpect(jsonPath("$.afternoonRemaining").value(3));

        long afternoonDogId = createDog(true, CENTER_CIF);
        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(afternoonDogId, groupId, date, "TARDE"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isCreated());

        long fullDayDogId = createDog(true, CENTER_CIF);
        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(fullDayDogId, groupId, date, "DIA_COMPLETO"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_FULL"));
    }

    @Test
    void mixedOccupancyAndHistoricalHalfDayConsumeBothSlots() throws Exception {
        long groupId = createGroup(3);
        LocalDate date = nextOpenDate();
        insertReservations(groupId, date, "DIA_COMPLETO", "COMPLETADA", 1);
        insertReservations(groupId, date, "MANANA", "PENDIENTE", 1);
        insertReservations(groupId, date, "TARDE", "CONFIRMADA", 1);

        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", date.toString())
                        .queryParam("groupId", Long.toString(groupId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morningRemaining").value(1))
                .andExpect(jsonPath("$.afternoonRemaining").value(1));

        long fullDayDogId = createDog(true, CENTER_CIF);
        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(fullDayDogId, groupId, date, "DIA_COMPLETO"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isCreated());

        long legacyGroupId = createGroup(3);
        LocalDate legacyDate = nextOpenDate().plusWeeks(1);
        insertReservations(legacyGroupId, legacyDate, "MEDIA_JORNADA", "CONFIRMADA", 1);
        insertReservations(legacyGroupId, legacyDate, "DIA_COMPLETO", "CANCELADA", 2);
        this.mockMvc.perform(get("/api/recepcion/disponibilidad")
                        .queryParam("date", legacyDate.toString())
                        .queryParam("groupId", Long.toString(legacyGroupId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morningRemaining").value(2))
                .andExpect(jsonPath("$.afternoonRemaining").value(2));
    }

    @Test
    void activeDuplicateIsRejectedButCancelledHistoryCanBeRebooked() throws Exception {
        long groupId = createGroup(4);
        long dogId = createDog(true, CENTER_CIF);
        LocalDate date = nextOpenDate();
        insertReservation(dogId, groupId, date, "DIA_COMPLETO", "CANCELADA");

        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(dogId, groupId, date, "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isCreated());

        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(dogId, groupId, date, "TARDE"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESERVATION"));

        assertEquals(2, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reserva WHERE perro_id = ? AND fecha = ?",
                Integer.class,
                dogId,
                date
        ));
    }

    @Test
    void datesAndReservationCandidatesAreValidated() throws Exception {
        long groupId = createGroup(3);
        long dogId = createDog(true, CENTER_CIF);
        LocalDate today = LocalDate.now(this.receptionClock);

        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            this.mockMvc.perform(post("/api/recepcion/reservas")
                            .contentType(APPLICATION_JSON)
                            .content(reservationRequest(dogId, groupId, today, "DIA_COMPLETO"))
                            .header("Authorization", basicAuthorization("recepcionista")))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("DATE_NOT_ALLOWED"));
        } else {
            this.mockMvc.perform(post("/api/recepcion/reservas")
                            .contentType(APPLICATION_JSON)
                            .content(reservationRequest(dogId, groupId, today, "DIA_COMPLETO"))
                            .header("Authorization", basicAuthorization("recepcionista")))
                    .andExpect(status().isCreated());
        }

        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(createDog(true, CENTER_CIF), groupId, today.minusDays(1), "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DATE_NOT_ALLOWED"));

        this.mockMvc.perform(get("/api/recepcion/perros")
                        .queryParam("reservationCandidates", "true")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].tutorId").isNotEmpty());

        long dogWithoutTutor = createDog(false, CENTER_CIF);
        this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(dogWithoutTutor, groupId, nextOpenDate(), "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    @Test
    void competingRequestsForAnEmptyCapacityRowCannotOverbook() throws Exception {
        long groupId = createGroup(1);
        long firstDogId = createDog(true, CENTER_CIF);
        long secondDogId = createDog(true, CENTER_CIF);
        LocalDate date = nextOpenDate().plusWeeks(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> createAtSameTime(
                    startSignal, firstDogId, groupId, date
            ));
            Future<Integer> second = executor.submit(() -> createAtSameTime(
                    startSignal, secondDogId, groupId, date
            ));
            startSignal.countDown();
            List<Integer> statuses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(status -> status == 201).count());
            assertEquals(1, statuses.stream().filter(status -> status == 409).count());
            assertEquals(1, this.jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reserva WHERE grupo_id = ? AND fecha = ? AND estado <> 'CANCELADA'",
                    Integer.class,
                    groupId,
                    date
            ));
        } finally {
            executor.shutdownNow();
        }
    }

    private int createAtSameTime(
            CountDownLatch startSignal,
            long dogId,
            long groupId,
            LocalDate date
    ) throws Exception {
        assertTrue(startSignal.await(10, TimeUnit.SECONDS));
        MvcResult result = this.mockMvc.perform(post("/api/recepcion/reservas")
                        .contentType(APPLICATION_JSON)
                        .content(reservationRequest(dogId, groupId, date, "MANANA"))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private void insertReservations(
            long groupId,
            LocalDate date,
            String modality,
            String status,
            int amount
    ) {
        for (int index = 0; index < amount; index++) {
            long dogId = createDog(true, CENTER_CIF);
            insertReservation(dogId, groupId, date, modality, status);
        }
    }

    private void insertReservation(
            long dogId,
            long groupId,
            LocalDate date,
            String modality,
            String status
    ) {
        this.jdbcTemplate.update("""
                INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
                VALUES (?, ?, ?, ?, ?)
                """, dogId, groupId, date, modality, status);
    }

    private long createDog(boolean withTutor, String centerCif) {
        long centerId = centerId(centerCif);
        Long tutorId = null;
        if (withTutor) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            this.jdbcTemplate.update("""
                    INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                    VALUES (?, 'Tutor', 'Reserva', ?, ?, '981000000')
                    """, centerId, "BOOK-" + suffix, suffix + "@example.test");
            tutorId = this.jdbcTemplate.queryForObject(
                    "SELECT id FROM tutor WHERE dni = ?",
                    Long.class,
                    "BOOK-" + suffix
            );
        }
        String dogName = "Perro-" + UUID.randomUUID().toString().substring(0, 8);
        this.jdbcTemplate.update("""
                INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
                VALUES (?, ?, ?, 'MEDIANO', TRUE)
                """, centerId, tutorId, dogName);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                dogName
        );
    }

    private long createGroup(int capacity) {
        return createGroup(CENTER_CIF, capacity);
    }

    private long createGroup(String centerCif, int capacity) {
        long centerId = centerId(centerCif);
        String groupName = "Booking-" + UUID.randomUUID().toString().substring(0, 8);
        this.jdbcTemplate.update("""
                INSERT INTO grupo (centro_id, nombre, capacidad_max)
                VALUES (?, ?, ?)
                """, centerId, groupName, capacity);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM grupo WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                groupName
        );
    }

    private String createForeignCenter() {
        String cif = "F" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        this.jdbcTemplate.update("""
                INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                VALUES ('Centro externo de prueba', 'CLIENTE', ?, 'Dirección ficticia', TRUE)
                """, cif);
        return cif;
    }

    private long centerId(String cif) {
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?",
                Long.class,
                cif
        );
    }

    private LocalDate nextOpenDate() {
        LocalDate date = LocalDate.now(this.receptionClock);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String reservationRequest(long dogId, long groupId, LocalDate date, String modality) {
        return """
                {"dogId":%d,"groupId":%d,"date":"%s","modality":"%s"}
                """.formatted(dogId, groupId, date, modality);
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }
}
