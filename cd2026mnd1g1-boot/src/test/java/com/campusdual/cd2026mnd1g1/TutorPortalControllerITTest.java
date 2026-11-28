package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TutorPortalControllerITTest {

    private static final String CENTER_CIF = "B00000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unauthenticatedAndNonTutorUsersCannotUseTutorPortal() throws Exception {
        this.mockMvc.perform(get("/api/tutor/mis-perros"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/tutor/mis-perros")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountIdentityAndDogListAreScopedAndContainOnlySummaryFields() throws Exception {
        TutorAccount owner = createTutorAccount();
        long foreignCenterId = createForeignCenter();
        TutorAccount otherOwner = createTutorAccount(foreignCenterId);
        long ownDogId = createDog(owner.tutorId(), "Luna", "portal-dog.png");
        long foreignDogId = createDog(otherOwner.tutorId(), "Luna", null);

        this.jdbcTemplate.update("""
                INSERT INTO alergia (perro_id, descripcion)
                VALUES (?, 'Alergia de integración')
                """, ownDogId);
        this.jdbcTemplate.update("""
                INSERT INTO medicacion (perro_id, medicamento, dosis, pauta)
                VALUES (?, 'Medicación de prueba', '2 ml', 'Una vez al día')
                """, ownDogId);
        this.jdbcTemplate.update("""
                INSERT INTO incompatibilidad (perro_a_id, perro_b_id, motivo)
                VALUES (?, ?, 'Nota interna de integración')
                """, Math.min(ownDogId, foreignDogId), Math.max(ownDogId, foreignDogId));
        int dogsBeforePortalReads = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM perro", Integer.class);

        this.mockMvc.perform(get("/api/users/me")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(owner.username()))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_TUTOR"));

        this.mockMvc.perform(get("/api/users")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isForbidden());

        this.mockMvc.perform(get("/api/tutor/mis-perros")
                        .queryParam("tutor_id", Long.toString(otherOwner.tutorId()))
                        .queryParam("centro_id", Long.toString(foreignCenterId))
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ownDogId))
                .andExpect(jsonPath("$.items[0].name").value("Luna"))
                .andExpect(jsonPath("$.items[0].photoAvailable").value(true))
                .andExpect(jsonPath("$.items[0].tutorId").doesNotExist())
                .andExpect(jsonPath("$.items[0].temperament").doesNotExist())
                .andExpect(jsonPath("$.items[0].allergies").doesNotExist());

        this.mockMvc.perform(get("/api/tutor/mis-perros/{id}", ownDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.feedingInstructions").value("Alimentación compartida"))
                .andExpect(jsonPath("$.temperament").doesNotExist())
                .andExpect(jsonPath("$.allergies[0].description").value("Alergia de integración"))
                .andExpect(jsonPath("$.medications[0].medication").value("Medicación de prueba"))
                .andExpect(jsonPath("$.medications[0].dosage").value("2 ml"))
                .andExpect(jsonPath("$.incompatibilities").doesNotExist())
                .andExpect(jsonPath("$.weight").doesNotExist())
                .andExpect(jsonPath("$.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());

        this.mockMvc.perform(get("/api/tutor/mis-perros/{id}", foreignDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/tutor/mis-perros/{id}/foto", foreignDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/tutor/mis-perros/{id}/foto", ownDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay una foto disponible para este perro"));
        assertEquals(dogsBeforePortalReads,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM perro", Integer.class));
    }

    @Test
    void emptyListIsDifferentFromUnlinkedTutorAccount() throws Exception {
        TutorAccount withoutDogs = createTutorAccount();
        this.mockMvc.perform(get("/api/tutor/mis-perros")
                        .header("Authorization", basicAuthorization(withoutDogs.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        this.mockMvc.perform(get("/api/tutor/mis-perros")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TUTOR_ACCOUNT_NOT_LINKED"));

        int dogsBeforeCreate = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM perro", Integer.class);
        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart("""
                                {"name": "No vinculado", "size": "PEQUENO"}
                                """))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TUTOR_ACCOUNT_NOT_LINKED"));
        assertEquals(dogsBeforeCreate,
                this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM perro", Integer.class));
    }

    @Test
    void tutorReadsOnlyPublishedOwnSummariesAndCountChangesPerTutor() throws Exception {
        TutorAccount owner = createTutorAccount();
        TutorAccount otherOwner = createTutorAccount();
        long ownDogId = createDog(owner.tutorId(), "Resumen propio", null);
        long emptyDogId = createDog(owner.tutorId(), "Sin resúmenes", null);
        long foreignDogId = createDog(otherOwner.tutorId(), "Resumen ajeno", null);
        long olderReservationId = createReservation(ownDogId, LocalDate.now().minusDays(2));
        long newerReservationId = createReservation(ownDogId, LocalDate.now().minusDays(1));
        long unpublishedReservationId = createReservation(ownDogId, LocalDate.now().minusDays(3));
        long foreignReservationId = createReservation(foreignDogId, LocalDate.now().minusDays(1));
        long olderSummaryId = createDailySummary(olderReservationId, "PUBLICADO", "Resumen anterior");
        long newerSummaryId = createDailySummary(newerReservationId, "PUBLICADO", "Resumen reciente");
        long unpublishedSummaryId = createDailySummary(
                unpublishedReservationId,
                null,
                "Resumen no publicado"
        );
        long foreignSummaryId = createDailySummary(foreignReservationId, "PUBLICADO", "Resumen ajeno");

        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization(otherOwner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes", ownDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newerSummaryId))
                .andExpect(jsonPath("$[0].dogName").value("Resumen propio"))
                .andExpect(jsonPath("$[0].status").value("PUBLICADO"))
                .andExpect(jsonPath("$[0].viewed").value(false))
                .andExpect(jsonPath("$[1].id").value(olderSummaryId));
        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes", emptyDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}", ownDogId, newerSummaryId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Resumen reciente"))
                .andExpect(jsonPath("$.viewed").value(false))
                .andExpect(jsonPath("$.date").value(LocalDate.now().minusDays(1).toString()));

        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}", ownDogId, unpublishedSummaryId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}", ownDogId, foreignSummaryId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes", foreignDogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post(
                                "/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}/lectura",
                                foreignDogId,
                                foreignSummaryId
                        )
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(get("/api/tutor/notificaciones/contador"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isForbidden());
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization("tutor")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TUTOR_ACCOUNT_NOT_LINKED"));

        this.mockMvc.perform(
                        post("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}/lectura",
                                ownDogId,
                                newerSummaryId)
                                .header("Authorization", basicAuthorization(owner.username()))
                )
                .andExpect(status().isNoContent());
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
        this.mockMvc.perform(get("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}", ownDogId, newerSummaryId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewed").value(true));

        this.mockMvc.perform(
                        post("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}/lectura",
                                ownDogId,
                                newerSummaryId)
                                .header("Authorization", basicAuthorization(owner.username()))
                )
                .andExpect(status().isNoContent());
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
        this.mockMvc.perform(
                        post("/api/tutor/mis-perros/{dogId}/resumenes/{summaryId}/lectura",
                                ownDogId,
                                olderSummaryId)
                                .header("Authorization", basicAuthorization(owner.username()))
                )
                .andExpect(status().isNoContent());
        this.mockMvc.perform(get("/api/tutor/notificaciones/contador")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resumen_diario_lectura WHERE resumen_id = ? AND tutor_id = ?",
                Integer.class,
                newerSummaryId,
                owner.tutorId()
        ));
    }

    @Test
    void tutorCreatesDogAndAuthorizedPersonsAtomicallyAndRetriesIdempotently() throws Exception {
        TutorAccount owner = createTutorAccount();
        String idempotencyKey = UUID.randomUUID().toString();
        String request = """
                {
                  "name": "Nube",
                  "breed": "Mestiza",
                  "size": "MEDIANO",
                  "birthDate": "2022-05-04",
                  "allergies": [{"description": "Reacción declarada"}],
                  "medications": [{
                    "medication": "Tratamiento informado",
                    "dosage": "Una medida",
                    "instructions": "Pauta comunicada por el tutor"
                  }],
                  "authorizedPersons": [
                    {"name": "Persona autorizada", "phone": "+1 202 555 0188"},
                    {"name": "", "phone": ""}
                  ]
                }
                """;
        MockMultipartFile requestPart = jsonRequestPart(request);

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(requestPart)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nube"))
                .andExpect(jsonPath("$.allergies[0].description").value("Reacción declarada"))
                .andExpect(jsonPath("$.medications[0].medication").value("Tratamiento informado"))
                .andExpect(jsonPath("$.authorizedPersons[0].name").value("Persona autorizada"))
                .andExpect(jsonPath("$.authorizedPersons[0].phone").value("+1 202 555 0188"));

        Long dogId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE tutor_id = ? AND nombre = 'Nube'",
                Long.class,
                owner.tutorId()
        );
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT informacion_declarada_por_tutor FROM perro WHERE id = ?",
                Boolean.class,
                dogId
        ) ? 1 : 0);
        assertEquals(1, this.jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM persona_autorizada
                WHERE tutor_id = ? AND nombre = 'Persona autorizada' AND registrado_por_usuario_id =
                    (SELECT id FROM usuarios WHERE username = ?)
                """,
                Integer.class,
                owner.tutorId(),
                owner.username()
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tutor_dog_submission WHERE tutor_id = ? AND idempotency_key = ?",
                Integer.class,
                owner.tutorId(),
                idempotencyKey
        ));

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart(request))
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(dogId));

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart(request.replace("Nube", "Nube diferente")))
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE tutor_id = ? AND nombre = 'Nube'",
                Integer.class,
                owner.tutorId()
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM persona_autorizada WHERE tutor_id = ?",
                Integer.class,
                owner.tutorId()
        ));
        this.mockMvc.perform(get("/api/tutor/mis-perros/personas-autorizadas")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Persona autorizada"))
                .andExpect(jsonPath("$[0].phone").value("+1 202 555 0188"));
        this.mockMvc.perform(get("/api/tutor/mis-perros")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(dogId));
        this.mockMvc.perform(get("/api/recepcion/perros/{id}", dogId)
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizedPersons[0].name").value("Persona autorizada"))
                .andExpect(jsonPath("$.authorizedPersons[0].phone").value("+1 202 555 0188"));
    }

    @Test
    void differentTutorsCanCreateDogsWithTheSameName() throws Exception {
        TutorAccount firstOwner = createTutorAccount();
        TutorAccount secondOwner = createTutorAccount();
        String request = """
                {"name": "Luna", "size": "MEDIANO"}
                """;

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart(request))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", basicAuthorization(firstOwner.username())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luna"));

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart(request))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", basicAuthorization(secondOwner.username())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luna"));

        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE tutor_id = ? AND nombre = 'Luna'",
                Integer.class,
                firstOwner.tutorId()
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE tutor_id = ? AND nombre = 'Luna'",
                Integer.class,
                secondOwner.tutorId()
        ));
    }

    @Test
    void tutorDogCreationRollsBackWhenAnAuthorizedPersonIsIncomplete() throws Exception {
        TutorAccount owner = createTutorAccount();
        String idempotencyKey = UUID.randomUUID().toString();
        String invalidRequest = """
                {
                  "name": "No debe persistir",
                  "size": "PEQUENO",
                  "authorizedPersons": [{"name": "Nombre sin teléfono", "phone": ""}]
                }
                """;

        this.mockMvc.perform(multipart("/api/tutor/mis-perros")
                        .file(jsonRequestPart(invalidRequest))
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isBadRequest());

        assertEquals(0, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE tutor_id = ? AND nombre = 'No debe persistir'",
                Integer.class,
                owner.tutorId()
        ));
        assertEquals(0, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tutor_dog_submission WHERE tutor_id = ? AND idempotency_key = ?",
                Integer.class,
                owner.tutorId(),
                idempotencyKey
        ));
    }

    @Test
    void tutorUpdatesOnlyTheirDogAndPreservesCenterCareRecords() throws Exception {
        TutorAccount owner = createTutorAccount();
        long dogId = createDog(owner.tutorId(), "Antes", null);
        long otherDogId = createDog(owner.tutorId(), "Sin cambios", null);
        this.jdbcTemplate.update("""
                INSERT INTO alergia (perro_id, descripcion, declarada_por_tutor)
                VALUES (?, 'Alergia del centro', FALSE)
                """, dogId);
        long centerAllergyId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM alergia WHERE perro_id = ? AND descripcion = 'Alergia del centro'",
                Long.class,
                dogId
        );
        this.jdbcTemplate.update("""
                INSERT INTO medicacion (perro_id, medicamento, dosis, pauta, declarada_por_tutor)
                VALUES (?, 'Medicación del centro', '1 ml', 'Pauta interna', FALSE)
                """, dogId);
        this.jdbcTemplate.update("""
                INSERT INTO alergia (perro_id, descripcion, declarada_por_tutor)
                VALUES (?, 'Alergia tutor anterior', TRUE)
                """, dogId);
        this.jdbcTemplate.update("""
                INSERT INTO medicacion (perro_id, medicamento, dosis, pauta, declarada_por_tutor)
                VALUES (?, 'Medicación tutor anterior', '2 ml', 'Pauta declarada', TRUE)
                """, dogId);
        this.jdbcTemplate.update("""
                INSERT INTO persona_autorizada (tutor_id, nombre, telefono, registrado_por_usuario_id, creado_en)
                VALUES (?, 'Persona inicial', '5550101',
                        (SELECT id FROM usuarios WHERE username = ?), CURRENT_TIMESTAMP)
                """, owner.tutorId(), owner.username());
        Long existingAuthorizedId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM persona_autorizada WHERE tutor_id = ? AND nombre = 'Persona inicial'",
                Long.class,
                owner.tutorId()
        );

        MvcResult detailResult = this.mockMvc.perform(get("/api/tutor/mis-perros/{id}", dogId)
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperament").doesNotExist())
                .andExpect(jsonPath("$.allergies[0].description").value("Alergia del centro"))
                .andReturn();
        String etag = detailResult.getResponse().getHeader("ETag");

        String request = """
                {
                  "name": "Después",
                  "breed": "Mestiza",
                  "size": "GRANDE",
                  "birthDate": "2021-06-03",
                  "allergies": [{"description": "Alergia tutor nueva"}],
                  "medications": [{
                    "medication": "Medicación tutor nueva",
                    "dosage": null,
                    "instructions": "Pauta comunicada"
                  }],
                  "authorizedPersons": [
                    {"id": %d, "name": "Persona actualizada", "phone": "5550202"},
                    {"id": null, "name": "Persona nueva", "phone": "5550303"}
                  ],
                  "removePhoto": false
                }
                """.formatted(existingAuthorizedId);

        MvcResult updateResult = this.mockMvc.perform(multipart("/api/tutor/mis-perros/{id}", dogId)
                        .file(jsonRequestPart(request))
                        .header("If-Match", etag)
                        .header("Authorization", basicAuthorization(owner.username()))
                        .with(servletRequest -> {
                            servletRequest.setMethod("PUT");
                            return servletRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.name").value("Después"))
                .andExpect(jsonPath("$.temperament").doesNotExist())
                .andExpect(jsonPath("$.allergies.length()").value(2))
                .andExpect(jsonPath("$.allergies[0].description").value("Alergia del centro"))
                .andExpect(jsonPath("$.allergies[0].declaredByTutor").value(false))
                .andExpect(jsonPath("$.medications.length()").value(2))
                .andExpect(jsonPath("$.authorizedPersons.length()").value(2))
                .andReturn();

        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alergia WHERE id = ? AND declarada_por_tutor = FALSE",
                Integer.class,
                centerAllergyId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medicacion WHERE perro_id = ? AND medicamento = 'Medicación del centro'",
                Integer.class,
                dogId
        ));
        assertEquals("Alimentación compartida", this.jdbcTemplate.queryForObject(
                "SELECT pautas_alimentacion FROM perro WHERE id = ?",
                String.class,
                dogId
        ));
        assertEquals("Temperamento compartido", this.jdbcTemplate.queryForObject(
                "SELECT temperamento FROM perro WHERE id = ?",
                String.class,
                dogId
        ));
        assertEquals(0, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alergia WHERE perro_id = ? AND descripcion = 'Alergia tutor anterior'",
                Integer.class,
                dogId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perro WHERE id = ? AND nombre = 'Sin cambios'",
                Integer.class,
                otherDogId
        ));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM persona_autorizada WHERE id = ? AND nombre = 'Persona actualizada'",
                Integer.class,
                existingAuthorizedId
        ));

        long newAuthorizedId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM persona_autorizada WHERE tutor_id = ? AND nombre = 'Persona nueva'",
                Long.class,
                owner.tutorId()
        );
        String updatedEtag = updateResult.getResponse().getHeader("ETag");
        String revokeRequest = """
                {
                  "name": "Después",
                  "breed": "Mestiza",
                  "size": "GRANDE",
                  "birthDate": "2021-06-03",
                  "allergies": [{"description": "Alergia tutor nueva"}],
                  "medications": [{
                    "medication": "Medicación tutor nueva",
                    "dosage": null,
                    "instructions": "Pauta comunicada"
                  }],
                  "authorizedPersons": [
                    {"id": %d, "name": "Persona nueva", "phone": "5550303"}
                  ],
                  "removePhoto": false
                }
                """.formatted(newAuthorizedId);
        this.mockMvc.perform(multipart("/api/tutor/mis-perros/{id}", dogId)
                        .file(jsonRequestPart(revokeRequest))
                        .header("If-Match", updatedEtag)
                        .header("Authorization", basicAuthorization(owner.username()))
                        .with(servletRequest -> {
                            servletRequest.setMethod("PUT");
                            return servletRequest;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizedPersons.length()").value(1));
        assertEquals(1, this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM persona_autorizada WHERE id = ? AND revocado_en IS NOT NULL",
                Integer.class,
                existingAuthorizedId
        ));
        this.mockMvc.perform(get("/api/tutor/mis-perros/personas-autorizadas")
                        .header("Authorization", basicAuthorization(owner.username())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Persona nueva"));

        this.mockMvc.perform(multipart("/api/tutor/mis-perros/{id}", dogId)
                        .file(jsonRequestPart(request))
                        .header("If-Match", etag)
                        .header("Authorization", basicAuthorization(owner.username()))
                        .with(servletRequest -> {
                            servletRequest.setMethod("PUT");
                            return servletRequest;
                        }))
                .andExpect(status().isConflict());
    }

    private MockMultipartFile jsonRequestPart(String request) {
        return new MockMultipartFile(
                "request",
                "request.json",
                "application/json",
                request.getBytes(StandardCharsets.UTF_8)
        );
    }

    private TutorAccount createTutorAccount() {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        return createTutorAccount(centerId);
    }

    private TutorAccount createTutorAccount(long centerId) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String username = "portal-it-" + token;
        String nationalId = "PIT-" + token;
        Long tutorRoleId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM rol WHERE nombre = 'TUTOR'", Long.class
        );
        this.jdbcTemplate.update("""
                INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor integración', 'Portal', ?, ?, '5550100')
                """, centerId, "PT-" + token, username + "@example.test");
        Long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE centro_id = ? AND dni = ?",
                Long.class,
                centerId,
                "PT-" + token
        );
        this.jdbcTemplate.update("""
                INSERT INTO usuarios
                    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo, tutor_id)
                VALUES (?, ?, 'Tutor integración', 'Portal', ?, ?, ?, ?, TRUE, ?)
                """,
                centerId,
                tutorRoleId,
                nationalId,
                username + "@example.test",
                username,
                this.passwordEncoder.encode("admin1234"),
                tutorId
        );
        return new TutorAccount(username, tutorId);
    }

    private long createForeignCenter() {
        String centerCif = "B99999999";
        Integer centerCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM centro WHERE cif = ?", Integer.class, centerCif
        );
        if (centerCount == null || centerCount == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO centro (nombre, tipo, cif, direccion, activo)
                    VALUES ('Centro ajeno portal', 'CLIENTE', ?, 'Dirección de prueba', TRUE)
                    """, centerCif);
        }
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, centerCif
        );
    }

    private long createDog(long tutorId, String name, String photoUrl) {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT centro_id FROM tutor WHERE id = ?", Long.class, tutorId
        );
        this.jdbcTemplate.update("""
                INSERT INTO perro
                    (centro_id, tutor_id, nombre, raza, tamano, temperamento,
                     pautas_alimentacion, foto_url, activo)
                VALUES (?, ?, ?, 'Raza de prueba', 'MEDIANO', 'Temperamento compartido',
                        'Alimentación compartida', ?, TRUE)
                """, centerId, tutorId, name, photoUrl);
        return this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM perro WHERE tutor_id = ? AND nombre = ?",
                Long.class,
                tutorId,
                name
        );
    }

    private long createReservation(long dogId, LocalDate date) {
        this.jdbcTemplate.update("""
                INSERT INTO reserva (perro_id, fecha, modalidad, estado)
                VALUES (?, ?, 'DIA_COMPLETO', 'COMPLETADA')
                """, dogId, date);
        return this.jdbcTemplate.queryForObject(
                "SELECT id FROM reserva WHERE perro_id = ? AND fecha = ?",
                Long.class,
                dogId,
                date
        );
    }

    private long createDailySummary(long reservationId, String status, String text) {
        this.jdbcTemplate.update("""
                INSERT INTO resumen_diario
                    (reserva_id, destinatario, texto_ia, estado, generado_en, publicado_en)
                VALUES (?, 'TUTOR', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, reservationId, text, status);
        return this.jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM resumen_diario WHERE reserva_id = ? AND destinatario = 'TUTOR'",
                Long.class,
                reservationId
        );
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    private record TutorAccount(String username, long tutorId) {
    }
}
