package com.campusdual.cd2026mnd1g1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CaregiverPortalControllerITTest {

    private static final String CENTER_CIF = "B00000000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void caregiverListAndDetailAreScopedByAssignedGroup() throws Exception {
        String caregiver = createCaregiver("A");
        String otherCaregiver = createCaregiver("B");
        Long reservationId = createAssignedStay(caregiver, "Luna", "Tranquila", "Dos raciones");
        Long otherReservationId = createAssignedStay(otherCaregiver, "Milo", "Reservado", "Una ración");

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy")
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].reservationId").value(reservationId))
                .andExpect(jsonPath("$.items[0].name").value("Luna"))
                .andExpect(jsonPath("$.items[0].temperament").doesNotExist())
                .andExpect(jsonPath("$.items[0].feedingInstructions").doesNotExist());

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy/{reservationId}", reservationId)
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.temperament").value("Tranquila"))
                .andExpect(jsonPath("$.feedingInstructions").value("Dos raciones"))
                .andExpect(jsonPath("$.tutor.name").exists())
                .andExpect(jsonPath("$.tutor.phone").value("5550100"))
                .andExpect(jsonPath("$.allergies").isArray())
                .andExpect(jsonPath("$.medications").isArray())
                .andExpect(jsonPath("$.incompatibilities").isArray())
                .andExpect(jsonPath("$.tutorId").doesNotExist());

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy/{reservationId}", otherReservationId)
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/registros-diarios", reservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"type":"COMIDA","notes":"Comió con normalidad"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.type").value("COMIDA"))
                .andExpect(jsonPath("$.notes").value("Comió con normalidad"));

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/registros-diarios", reservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"type":"PASEO","notes":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").doesNotExist());

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy/{reservationId}/registros-diarios", reservationId)
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/registros-diarios", otherReservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"type":"OTRO","notes":"Intento ajeno"}
                                """))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/resumen", reservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"text":"Comió con normalidad y realizó paseos tranquilos."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.status").value("PUBLICADO"))
                .andExpect(jsonPath("$.text").value("Comió con normalidad y realizó paseos tranquilos."));

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy/{reservationId}/resumen", reservationId)
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Comió con normalidad y realizó paseos tranquilos."));

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/resumen", reservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"text":"Intento de sustitución"}
                                """))
                .andExpect(status().isConflict());

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/resumen", otherReservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"text":"Resumen ajeno"}
                                """))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/incidencias", reservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"severity":"MODERADA","description":"Se observó incomodidad durante el paseo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.severity").value("MODERADA"))
                .andExpect(jsonPath("$.description").value("Se observó incomodidad durante el paseo"));

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy/{reservationId}/incidencias", reservationId)
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        this.mockMvc.perform(post("/api/cuidador/mis-perros-hoy/{reservationId}/incidencias", otherReservationId)
                        .header("Authorization", basicAuthorization(caregiver))
                        .contentType("application/json")
                        .content("""
                                {"severity":"GRAVE","description":"Incidencia ajena"}
                                """))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy")
                        .header("Authorization", basicAuthorization("recepcionista")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAndCaregiverWithoutAssignmentAreHandled() throws Exception {
        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy"))
                .andExpect(status().isUnauthorized());

        String caregiver = createCaregiver("C");
        this.jdbcTemplate.update("""
                UPDATE grupo SET cuidador_id = NULL
                WHERE centro_id = (SELECT id FROM centro WHERE cif = ?)
                  AND cuidador_id = (SELECT id FROM usuarios WHERE username = ?)
                """, CENTER_CIF, caregiver);

        this.mockMvc.perform(get("/api/cuidador/mis-perros-hoy")
                        .header("Authorization", basicAuthorization(caregiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Long createAssignedStay(
            String username,
            String dogName,
            String temperament,
            String feedingInstructions
    ) {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        Long caregiverId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM usuarios WHERE username = ?", Long.class, username
        );
        String token = UUID.randomUUID().toString().substring(0, 8);
        this.jdbcTemplate.update("""
                INSERT INTO tutor
                    (centro_id, nombre, apellidos, dni, email, telefono)
                VALUES (?, 'Tutor', 'Prueba', ?, ?, '5550100')
                """,
                centerId,
                "CGT-" + token,
                "tutor-" + token + "@example.test"
        );
        Long tutorId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM tutor WHERE centro_id = ? AND dni = ?",
                Long.class,
                centerId,
                "CGT-" + token
        );
        this.jdbcTemplate.update("""
                INSERT INTO grupo (centro_id, nombre, capacidad_max, cuidador_id)
                VALUES (?, ?, 10, ?)
                """, centerId, "Grupo " + token, caregiverId);
        Long groupId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM grupo WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                "Grupo " + token
        );
        this.jdbcTemplate.update("""
                INSERT INTO perro
                    (centro_id, tutor_id, nombre, raza, fecha_nacimiento, tamano,
                     temperamento, pautas_alimentacion, activo, creado_en)
                VALUES (?, ?, ?, 'Criollo', DATE '2021-01-01', 'MEDIANO', ?, ?, TRUE, CURRENT_TIMESTAMP)
                """, centerId, tutorId, dogName, temperament, feedingInstructions);
        Long dogId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM perro WHERE centro_id = ? AND nombre = ?",
                Long.class,
                centerId,
                dogName
        );
        this.jdbcTemplate.update("""
                INSERT INTO alergia (perro_id, descripcion, declarada_por_tutor)
                VALUES (?, 'Polen', FALSE)
                """, dogId);
        this.jdbcTemplate.update("""
                INSERT INTO medicacion (perro_id, medicamento, dosis, pauta, declarada_por_tutor)
                VALUES (?, 'Antiinflamatorio', '1 dosis', 'Según pauta', FALSE)
                """, dogId);
        this.jdbcTemplate.update("""
                INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado, creado_en)
                VALUES (?, ?, ?, 'DIA_COMPLETO', 'EN_CURSO', CURRENT_TIMESTAMP)
                """, dogId, groupId, LocalDate.now());
        Long reservationId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM reserva WHERE perro_id = ? AND fecha = ?",
                Long.class,
                dogId,
                LocalDate.now()
        );
        this.jdbcTemplate.update("""
                INSERT INTO check_registro
                    (reserva_id, usuario_id, tipo, fecha_hora, estado_mascota)
                VALUES (?, ?, 'ENTRADA', CURRENT_TIMESTAMP, 'Buen estado')
                """, reservationId, caregiverId);
        return reservationId;
    }

    private String createCaregiver(String suffix) {
        Long centerId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM centro WHERE cif = ?", Long.class, CENTER_CIF
        );
        Long caregiverRoleId = this.jdbcTemplate.queryForObject(
                "SELECT id FROM rol WHERE nombre = 'CUIDADOR'", Long.class
        );
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "caregiver-" + suffix.toLowerCase() + "-" + token;
        this.jdbcTemplate.update("""
                INSERT INTO usuarios
                    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
                VALUES (?, ?, 'Cuidador', ?, ?, ?, ?, ?, TRUE)
                """,
                centerId,
                caregiverRoleId,
                "Integración " + suffix,
                "CG-" + token,
                "cg-" + token + "@example.test",
                username,
                this.passwordEncoder.encode("admin1234")
        );
        return username;
    }

    private String basicAuthorization(String username) {
        String credentials = username + ":admin1234";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
