package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReceptionGroomingRequestControllerITTest {

    private static final String CENTER_CIF = "B00000000";
    private static final LocalDate REQUEST_DATE = LocalDate.of(2026, 11, 25);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/recepcion/peluqueros"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/recepcion/peluqueros/0/agenda")
                        .queryParam("date", REQUEST_DATE.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tutorRoleCannotReadReceptionGroomingRequests() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/1/confirmar")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/1/rechazar")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/peluqueros")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionListsAndFiltersInternalAndB2bRequestsWithinItsServiceCenter() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createCenter("B00000111", "CLIENTE", "Guardería B2B pruebas");
        long otherProviderId = createCenter("B00000112", "PROPIA", "Otro centro de peluquería");
        long internalTutorId = createTutor(ownCenterId, "GROOM-INT-01");
        long internalDogId = createDog(ownCenterId, internalTutorId, "Luna Grooming");
        long b2bDogId = createDog(clientCenterId, null, "Nube B2B");
        long otherDogId = createDog(otherProviderId, null, "Fuera de ámbito");
        long ownServiceId = createService(ownCenterId, "Baño y secado");
        long otherServiceId = createService(otherProviderId, "Corte");

        long internalRequestId = createRequest(
                ownCenterId, internalDogId, ownServiceId, REQUEST_DATE, "SOLICITADA", "Observación interna"
        );
        createRequest(
                clientCenterId, b2bDogId, ownServiceId, REQUEST_DATE.plusDays(1), "CONFIRMADA", null
        );
        long foreignRequestId = createRequest(
                otherProviderId, otherDogId, otherServiceId, REQUEST_DATE, "SOLICITADA", null
        );

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .queryParam("from", REQUEST_DATE.toString())
                        .queryParam("to", REQUEST_DATE.plusDays(1).toString())
                        .queryParam("centerId", Long.toString(otherProviderId))
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].origin").value("B2B"))
                .andExpect(jsonPath("$.items[0].customerName").value("Guardería B2B pruebas"))
                .andExpect(jsonPath("$.items[0].serviceNames[0]").value("Baño y secado"))
                .andExpect(jsonPath("$.items[1].id").value(internalRequestId))
                .andExpect(jsonPath("$.items[1].origin").value("INTERNA"))
                .andExpect(jsonPath("$.items[1].customerName").value("Tutor GROOM-INT-01"));

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .queryParam("origin", "B2B")
                        .queryParam("status", "CONFIRMADA")
                        .queryParam("from", REQUEST_DATE.plusDays(1).toString())
                        .queryParam("to", REQUEST_DATE.plusDays(1).toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].origin").value("B2B"))
                .andExpect(jsonPath("$.items[0].status").value("CONFIRMADA"));

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria/{id}", foreignRequestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailReturnsOnlyRequestedFieldsAndHidesIncompleteLegacyRequests() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long tutorId = createTutor(ownCenterId, "GROOM-INT-02");
        long dogId = createDog(ownCenterId, tutorId, "Toby detalle");
        long serviceId = createService(ownCenterId, "Deslanado");
        long requestId = createRequest(
                ownCenterId, dogId, serviceId, REQUEST_DATE, "SOLICITADA", "Revisar disponibilidad"
        );
        long incompleteRequestId = createIncompleteLegacyRequest(ownCenterId, REQUEST_DATE);

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria/{id}", requestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogName").value("Toby detalle"))
                .andExpect(jsonPath("$.customerName").value("Tutor GROOM-INT-02"))
                .andExpect(jsonPath("$.serviceNames[0]").value("Deslanado"))
                .andExpect(jsonPath("$.requestedTime").value("10:30:00"))
                .andExpect(jsonPath("$.notes").value("Revisar disponibilidad"))
                .andExpect(jsonPath("$.origin").value("INTERNA"))
                .andExpect(jsonPath("$.modality").doesNotExist())
                .andExpect(jsonPath("$.peluquero").doesNotExist());

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria/{id}", incompleteRequestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionCanConfirmAndRejectOnlyVisiblePendingRequests() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createCenter("B00000114", "CLIENTE", "Guardería B2B acción");
        long otherProviderId = createCenter("B00000115", "PROPIA", "Centro ajeno acción");
        long tutorId = createTutor(ownCenterId, "GROOM-ACT-01");
        long internalDogId = createDog(ownCenterId, tutorId, "Perro a confirmar");
        long b2bDogId = createDog(clientCenterId, null, "Perro B2B a rechazar");
        long externalDogId = createDog(otherProviderId, null, "Perro centro ajeno");
        long serviceId = createService(ownCenterId, "Servicio decisión");
        long otherServiceId = createService(otherProviderId, "Servicio externo decisión");
        long internalRequestId = createRequest(
                ownCenterId, internalDogId, serviceId, REQUEST_DATE, "SOLICITADA", "Observación conservada"
        );
        long b2bRequestId = createRequest(
                clientCenterId, b2bDogId, serviceId, REQUEST_DATE, "SOLICITADA", "Nota B2B conservada"
        );
        long foreignRequestId = createRequest(
                otherProviderId, externalDogId, otherServiceId, REQUEST_DATE, "SOLICITADA", "No accesible"
        );
        long appointmentCountBefore = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cita_peluqueria",
                Long.class
        );
        long requestCountBefore = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitud_peluqueria",
                Long.class
        );

        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/confirmar", internalRequestId)
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"RECHAZADA","perro_id":999999,"centro_id":999999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.notes").value("Observación conservada"));

        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/rechazar", b2bRequestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHAZADA"))
                .andExpect(jsonPath("$.notes").value("Nota B2B conservada"));

        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/rechazar", internalRequestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GROOMING_REQUEST_NOT_PENDING"));

        this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/confirmar", foreignRequestId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertEquals(
                "CONFIRMADA",
                this.jdbcTemplate.queryForObject(
                        "SELECT estado FROM solicitud_peluqueria WHERE id = ?",
                        String.class,
                        internalRequestId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "RECHAZADA",
                this.jdbcTemplate.queryForObject(
                        "SELECT estado FROM solicitud_peluqueria WHERE id = ?",
                        String.class,
                        b2bRequestId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Observación conservada",
                this.jdbcTemplate.queryForObject(
                        "SELECT notas FROM solicitud_peluqueria WHERE id = ?",
                        String.class,
                        internalRequestId
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                requestCountBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM solicitud_peluqueria", Long.class)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                appointmentCountBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cita_peluqueria", Long.class)
        );
    }

    @Test
    void requestsNotPendingCannotBeConfirmedOrRejected() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long dogId = createDog(ownCenterId, createTutor(ownCenterId, "GROOM-ACT-02"), "Perro no pendiente");
        long serviceId = createService(ownCenterId, "Servicio no pendiente");
        for (String requestStatus : new String[]{"CONFIRMADA", "COMPLETADA", "CANCELADA"}) {
            long requestId = createRequest(
                    ownCenterId, dogId, serviceId, REQUEST_DATE, requestStatus, null
            );
            this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/confirmar", requestId)
                            .header("Authorization", basicAuthorization("recepcionista")))
                    .andExpect(status().isConflict());
            this.mockMvc.perform(post("/api/recepcion/solicitudes-peluqueria/{id}/rechazar", requestId)
                            .header("Authorization", basicAuthorization("recepcionista")))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void dateRangeAndEnumFiltersAreValidated() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .queryParam("from", REQUEST_DATE.plusDays(1).toString())
                        .queryParam("to", REQUEST_DATE.toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());

        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .queryParam("origin", "NO_EXISTE")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filtersWithoutMatchesReturnAnEmptyPage() throws Exception {
        this.mockMvc.perform(get("/api/recepcion/solicitudes-peluqueria")
                        .queryParam("from", "2099-12-30")
                        .queryParam("to", "2099-12-31")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void receptionListsAndOpensPersistedGroomingAppointments() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long tutorId = createTutor(ownCenterId, "GROOM-APT-01");
        long dogId = createDog(ownCenterId, tutorId, "Cita real");
        long serviceId = createService(ownCenterId, "Corte real");
        long groomerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'",
                Long.class
        );
        long appointmentId = createAppointment(
                dogId,
                groomerId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 10, 30),
                "CONFIRMADA"
        );

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(appointmentId));

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria")
                        .queryParam("from", REQUEST_DATE.toString())
                        .queryParam("to", REQUEST_DATE.toString())
                        .queryParam("status", "CONFIRMADA")
                        .queryParam("centerId", "999999")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(appointmentId))
                .andExpect(jsonPath("$.items[0].dogName").value("Cita real"))
                .andExpect(jsonPath("$.items[0].serviceNames[0]").value("Corte real"))
                .andExpect(jsonPath("$.items[0].date").value("2026-11-25"))
                .andExpect(jsonPath("$.items[0].time").value("10:30:00"))
                .andExpect(jsonPath("$.items[0].origin").value("INTERNA"))
                .andExpect(jsonPath("$.items[0].status").value("CONFIRMADA"));

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria/{id}", appointmentId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.dogName").value("Cita real"))
                .andExpect(jsonPath("$.serviceNames[0]").value("Corte real"));
    }

    @Test
    void receptionCreatesInternalAppointmentWithoutAssigningGroomerOrLocation() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long tutorId = createTutor(ownCenterId, "GROOM-CREATE-01");
        long dogId = createDog(ownCenterId, tutorId, "Perro cita nueva");
        long serviceId = createService(ownCenterId, "Servicio cita nueva");
        long receptionistId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'",
                Long.class
        );
        long reservationsBefore = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reserva", Long.class);
        long requestsBefore = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitud_peluqueria",
                Long.class
        );
        String requestBody = """
                {
                  "dogId": %d,
                  "serviceId": %d,
                  "date": "2099-12-30",
                  "time": "10:30",
                  "notes": "Revisar el cepillado",
                  "status": "CONFIRMADA",
                  "centerId": 999999,
                  "receptionistId": 999999
                }
                """.formatted(dogId, serviceId);

        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.dogName").value("Perro cita nueva"))
                .andExpect(jsonPath("$.serviceNames[0]").value("Servicio cita nueva"))
                .andExpect(jsonPath("$.date").value("2099-12-30"))
                .andExpect(jsonPath("$.time").value("10:30:00"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.location").value(nullValue()))
                .andExpect(jsonPath("$.status").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.notes").value("Revisar el cepillado"));

        Long createdAppointmentId = this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM cita_peluqueria WHERE perro_id = ? AND inicio = ?",
                Long.class,
                dogId,
                LocalDateTime.of(2099, 12, 30, 10, 30)
        );
        org.junit.jupiter.api.Assertions.assertNotNull(createdAppointmentId);
        org.junit.jupiter.api.Assertions.assertEquals(
                receptionistId,
                this.jdbcTemplate.queryForObject(
                        "SELECT creado_por_usuario_id FROM cita_peluqueria WHERE id = ?",
                        Long.class,
                        createdAppointmentId
                )
        );
        org.junit.jupiter.api.Assertions.assertNull(this.jdbcTemplate.queryForObject(
                "SELECT peluquero_id FROM cita_peluqueria WHERE id = ?",
                Long.class,
                createdAppointmentId
        ));
        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria/{id}", createdAppointmentId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.notes").value("Revisar el cepillado"));
        org.junit.jupiter.api.Assertions.assertEquals(
                reservationsBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reserva", Long.class)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                requestsBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM solicitud_peluqueria", Long.class)
        );
    }

    @Test
    void groomingAppointmentOptionsAndCreationRejectExternalDogsAndInactiveServices() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createCenter("B00000114", "CLIENTE", "Guardería externa cita");
        long otherOwnCenterId = createCenter("B00000115", "PROPIA", "Otra guardería cita");
        long ownDogId = createDog(ownCenterId, createTutor(ownCenterId, "GROOM-CREATE-02"), "Perro interno selector");
        long externalDogId = createDog(clientCenterId, null, "Perro B2B no selector");
        long foreignDogId = createDog(otherOwnCenterId, null, "Perro de otro centro");
        long serviceId = createService(ownCenterId, "Servicio activo selector");
        long inactiveServiceId = createService(ownCenterId, "Servicio inactivo selector");
        this.jdbcTemplate.update("UPDATE servicio_peluqueria SET activo = FALSE WHERE id = ?", inactiveServiceId);

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria/perros")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + ownDogId + ")]").exists())
                .andExpect(jsonPath("$.items[?(@.id == " + externalDogId + ")]").doesNotExist())
                .andExpect(jsonPath("$.items[?(@.id == " + foreignDogId + ")]").doesNotExist());
        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria/servicios")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + serviceId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + inactiveServiceId + ")]").doesNotExist());

        String externalDogRequest = """
                {"dogId": %d, "serviceId": %d, "date": "2099-12-30", "time": "10:30"}
                """.formatted(externalDogId, serviceId);
        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(externalDogRequest))
                .andExpect(status().isNotFound());

        String foreignDogRequest = """
                {"dogId": %d, "serviceId": %d, "date": "2099-12-30", "time": "10:30"}
                """.formatted(foreignDogId, serviceId);
        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foreignDogRequest))
                .andExpect(status().isNotFound());

        String inactiveServiceRequest = """
                {"dogId": %d, "serviceId": %d, "date": "2099-12-30", "time": "10:30"}
                """.formatted(ownDogId, inactiveServiceId);
        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveServiceRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionListsOwnGroomersAndReadsTheirDailyAgendaAndDetails() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createCenter("B00000116", "CLIENTE", "Guardería B2B agenda");
        long otherCenterId = createCenter("B00000117", "PROPIA", "Otro centro peluquería agenda");
        long ownGroomerId = createGroomer(ownCenterId, "agenda-groomer-a", "Ada", "Peluquera");
        long emptyGroomerId = createGroomer(ownCenterId, "agenda-groomer-empty", "Eva", "Sin citas");
        long inactiveGroomerId = createGroomer(ownCenterId, "agenda-groomer-inactive", "Ines", "Inactiva");
        long foreignGroomerId = createGroomer(otherCenterId, "agenda-groomer-foreign", "Noa", "Ajena");
        this.jdbcTemplate.update("UPDATE usuarios SET activo = FALSE WHERE id = ?", inactiveGroomerId);
        long internalDogId = createDog(
                ownCenterId,
                createTutor(ownCenterId, "GROOM-AGENDA-01"),
                "Perro interno agenda"
        );
        long b2bDogId = createDog(clientCenterId, null, "Perro B2B agenda");
        long serviceId = createService(ownCenterId, "Servicio agenda");
        long foreignDogId = createDog(otherCenterId, null, "Perro otro centro agenda");
        long foreignServiceId = createService(otherCenterId, "Servicio otro centro agenda");
        long internalAppointmentId = createAppointment(
                internalDogId,
                ownGroomerId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 9, 0),
                "CONFIRMADA"
        );
        long b2bAppointmentId = createAppointment(
                b2bDogId,
                ownGroomerId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 11, 30),
                "COMPLETADA"
        );
        long foreignAppointmentId = createAppointment(
                foreignDogId,
                foreignGroomerId,
                foreignServiceId,
                LocalDateTime.of(2026, 11, 25, 14, 0),
                "CONFIRMADA"
        );
        createAppointment(
                internalDogId,
                ownGroomerId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 12, 0),
                "CANCELADA"
        );
        createUnassignedAppointment(
                internalDogId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 13, 0)
        );
        long appointmentsBefore = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cita_peluqueria",
                Long.class
        );
        long requestsBefore = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitud_peluqueria",
                Long.class
        );

        this.mockMvc.perform(get("/api/recepcion/peluqueros")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ownGroomerId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + emptyGroomerId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + inactiveGroomerId + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == " + foreignGroomerId + ")]").doesNotExist());

        this.mockMvc.perform(get("/api/recepcion/peluqueros/{id}/agenda", ownGroomerId)
                        .queryParam("date", REQUEST_DATE.toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groomerId").value(ownGroomerId))
                .andExpect(jsonPath("$.groomerName").value("Ada Peluquera"))
                .andExpect(jsonPath("$.date").value("2026-11-25"))
                .andExpect(jsonPath("$.appointments.length()").value(3))
                .andExpect(jsonPath("$.appointments[0].id").value(internalAppointmentId))
                .andExpect(jsonPath("$.appointments[0].origin").value("INTERNA"))
                .andExpect(jsonPath("$.appointments[0].status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.appointments[1].id").value(b2bAppointmentId))
                .andExpect(jsonPath("$.appointments[1].origin").value("B2B"))
                .andExpect(jsonPath("$.appointments[1].customerName").value("Guardería B2B agenda"))
                .andExpect(jsonPath("$.appointments[1].status").value("COMPLETADA"))
                .andExpect(jsonPath("$.appointments[2].status").value("CANCELADA"));

        this.mockMvc.perform(get("/api/recepcion/peluqueros/{id}/agenda", emptyGroomerId)
                        .queryParam("date", REQUEST_DATE.toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointments.length()").value(0));

        this.mockMvc.perform(get(
                                "/api/recepcion/peluqueros/{groomerId}/agenda/{appointmentId}",
                                ownGroomerId,
                                b2bAppointmentId
                        )
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogName").value("Perro B2B agenda"))
                .andExpect(jsonPath("$.customerName").value("Guardería B2B agenda"))
                .andExpect(jsonPath("$.serviceNames[0]").value("Servicio agenda"));

        this.mockMvc.perform(get(
                                "/api/recepcion/peluqueros/{groomerId}/agenda/{appointmentId}",
                                ownGroomerId,
                                internalAppointmentId
                        )
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dogName").value("Perro interno agenda"))
                .andExpect(jsonPath("$.customerName").value("Tutor GROOM-AGENDA-01"));

        this.mockMvc.perform(get("/api/recepcion/peluqueros/{id}/agenda", foreignGroomerId)
                        .queryParam("date", REQUEST_DATE.toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get(
                                "/api/recepcion/peluqueros/{groomerId}/agenda/{appointmentId}",
                                foreignGroomerId,
                                internalAppointmentId
                        )
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get(
                                "/api/recepcion/peluqueros/{groomerId}/agenda/{appointmentId}",
                                ownGroomerId,
                                foreignAppointmentId
                        )
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get(
                                "/api/recepcion/peluqueros/{groomerId}/agenda/{appointmentId}",
                                ownGroomerId,
                                internalAppointmentId
                        )
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/recepcion/peluqueros/{id}/agenda", ownGroomerId)
                        .queryParam("date", "invalid-date")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(
                appointmentsBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cita_peluqueria", Long.class)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                requestsBefore,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM solicitud_peluqueria", Long.class)
        );
    }

    @Test
    void groomerAgendaRejectsUnassignedAppointmentsAndMissingDate() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long groomerId = createGroomer(ownCenterId, "agenda-groomer-validation", "Mia", "Agenda");
        this.mockMvc.perform(get("/api/recepcion/peluqueros/{id}/agenda", groomerId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groomingAppointmentCreationRequiresReceptionAndRequiredFields() throws Exception {
        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("tutor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dogId": 0,
                                  "serviceId": 0,
                                  "date": "2099-12-30",
                                  "time": "10:30"
                                }
                                """))
                .andExpect(status().isForbidden());

        this.mockMvc.perform(post("/api/recepcion/citas-peluqueria")
                        .header("Authorization", basicAuthorization("recepcionista"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appointmentWithInconsistentOptionalRequestRemainsVisibleWithoutItsNotes() throws Exception {
        long ownCenterId = centerId(CENTER_CIF);
        long clientCenterId = createCenter("B00000113", "CLIENTE", "Centro cliente inconsistente");
        long tutorId = createTutor(ownCenterId, "GROOM-APT-02");
        long dogId = createDog(ownCenterId, tutorId, "Perro cita inconsistente");
        long serviceId = createService(ownCenterId, "Servicio cita inconsistente");
        long groomerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = 'recepcionista'",
                Long.class
        );
        this.jdbcTemplate.update("""
                INSERT INTO solicitud_peluqueria
                    (centro_cliente_id, modalidad, fecha_solicitada, estado, notas)
                VALUES (?, 'EN_INSTALACIONES', ?, 'SOLICITADA', 'Nota de solicitud ajena')
                """, clientCenterId, REQUEST_DATE);
        long inconsistentRequestId = this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM solicitud_peluqueria WHERE centro_cliente_id = ?",
                Long.class,
                clientCenterId
        );
        long appointmentId = createAppointment(
                dogId,
                groomerId,
                inconsistentRequestId,
                serviceId,
                LocalDateTime.of(2026, 11, 25, 12, 0),
                "SOLICITADA"
        );

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria")
                        .queryParam("from", REQUEST_DATE.toString())
                        .queryParam("to", REQUEST_DATE.toString())
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(appointmentId))
                .andExpect(jsonPath("$.items[0].dogName").value("Perro cita inconsistente"));

        this.mockMvc.perform(get("/api/recepcion/citas-peluqueria/{id}", appointmentId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId))
                .andExpect(jsonPath("$.notes").value(nullValue()));
    }

    private long createCenter(String cif, String type, String name) {
        this.jdbcTemplate.update("""
                INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                VALUES (?, ?, ?, 'Dirección de prueba', TRUE)
                """, name, type, cif);
        return centerId(cif);
    }

    private long createTutor(long centerId, String dni) {
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor', ?, ?, ?, '981000101')
                """, centerId, dni, dni, dni.toLowerCase() + "@example.test");
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE centro_id = ? AND dni = ?",
                Long.class,
                centerId,
                dni
        );
    }

    private long createDog(long centerId, Long tutorId, String name) {
        this.jdbcTemplate.update("""
                INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
                VALUES (?, ?, ?, 'MEDIANO', TRUE)
                """, centerId, tutorId, name);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                name
        );
    }

    private long createService(long centerId, String name) {
        this.jdbcTemplate.update("""
                INSERT INTO servicio_peluqueria (centro_id, nombre, duracion_min, precio, activo)
                VALUES (?, ?, 60, 35.00, TRUE)
                """, centerId, name);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM servicio_peluqueria WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                name
        );
    }

    private long createGroomer(long centerId, String username, String firstName, String lastName) {
        this.jdbcTemplate.update("""
                INSERT INTO usuarios (
                    centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo, creado_en
                )
                SELECT ?, role.id, ?, ?, ?, ?, ?, receptionist.password_hash, TRUE, CURRENT_TIMESTAMP
                FROM rol role
                JOIN usuarios receptionist ON receptionist.username = 'recepcionista'
                WHERE role.nombre = 'PELUQUERO'
                """,
                centerId,
                firstName,
                lastName,
                "GA" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                username + "@example.test",
                username
        );
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = ?",
                Long.class,
                username
        );
    }

    private long createUnassignedAppointment(long dogId, long serviceId, LocalDateTime startAt) {
        this.jdbcTemplate.update("""
                INSERT INTO cita_peluqueria
                    (perro_id, peluquero_id, inicio, duracion_min, lugar, estado)
                VALUES (?, NULL, ?, 60, NULL, 'PENDIENTE_ASIGNACION')
                """, dogId, startAt);
        long appointmentId = this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM cita_peluqueria WHERE perro_id = ? AND inicio = ?",
                Long.class,
                dogId,
                startAt
        );
        this.jdbcTemplate.update(
                "INSERT INTO cita_servicio (cita_id, servicio_id) VALUES (?, ?)",
                appointmentId,
                serviceId
        );
        return appointmentId;
    }

    private long createRequest(
            long customerCenterId,
            long dogId,
            long serviceId,
            LocalDate date,
            String status,
            String notes
    ) {
        this.jdbcTemplate.update("""
                INSERT INTO solicitud_peluqueria
                    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
                VALUES (?, ?, 'EN_INSTALACIONES', ?, '10:30:00', ?, ?)
                """, customerCenterId, dogId, date, status, notes);
        long requestId = this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM solicitud_peluqueria WHERE centro_cliente_id = ? AND perro_id = ?",
                Long.class,
                customerCenterId,
                dogId
        );
        this.jdbcTemplate.update("""
                INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id) VALUES (?, ?)
                """, requestId, serviceId);
        return requestId;
    }

    private long createAppointment(
            long dogId,
            long groomerId,
            long serviceId,
            LocalDateTime startAt,
            String status
    ) {
        return createAppointment(dogId, groomerId, null, serviceId, startAt, status);
    }

    private long createAppointment(
            long dogId,
            long groomerId,
            Long requestId,
            long serviceId,
            LocalDateTime startAt,
            String status
    ) {
        this.jdbcTemplate.update("""
                INSERT INTO cita_peluqueria
                    (perro_id, peluquero_id, solicitud_id, inicio, duracion_min, lugar, estado)
                VALUES (?, ?, ?, ?, 60, 'EN_INSTALACIONES', ?)
                """, dogId, groomerId, requestId, startAt, status);
        long appointmentId = this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM cita_peluqueria WHERE perro_id = ? AND peluquero_id = ?",
                Long.class,
                dogId,
                groomerId
        );
        this.jdbcTemplate.update(
                "INSERT INTO cita_servicio (cita_id, servicio_id) VALUES (?, ?)",
                appointmentId,
                serviceId
        );
        return appointmentId;
    }

    private long createIncompleteLegacyRequest(long customerCenterId, LocalDate date) {
        this.jdbcTemplate.update("""
                INSERT INTO solicitud_peluqueria
                    (centro_cliente_id, modalidad, fecha_solicitada, estado, notas)
                VALUES (?, 'EN_INSTALACIONES', ?, 'SOLICITADA', 'Solicitud heredada sin perro')
                """, customerCenterId, date);
        return this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM solicitud_peluqueria WHERE centro_cliente_id = ?",
                Long.class,
                customerCenterId
        );
    }

    private long centerId(String cif) {
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?",
                Long.class,
                cif
        );
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );
        return "Basic " + encodedCredentials;
    }
}
