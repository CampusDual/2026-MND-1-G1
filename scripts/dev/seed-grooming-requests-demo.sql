-- Solicitudes y citas ficticias de peluquería para HSQLDB local (perfil p).
-- Ejecutar después de seed-reception-demo.sql. No se ejecuta al arrancar.
-- Inserta registros una sola vez por fixture; no elimina ni actualiza datos manuales.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO servicio_peluqueria (centro_id, nombre, duracion_min, precio, activo)
SELECT c.id, 'Baño y secado Demo', 60, 35.00, TRUE
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM servicio_peluqueria s
      WHERE s.centro_id = c.id AND s.nombre = 'Baño y secado Demo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.service.bano-demo', MIN(s.id)
FROM centro c
JOIN servicio_peluqueria s ON s.centro_id = c.id AND s.nombre = 'Baño y secado Demo'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.service.bano-demo'
  )
GROUP BY c.id;

INSERT INTO perro (centro_id, nombre, tamano, activo)
SELECT c.id, 'Rizo B2B Demo', 'MEDIANO', TRUE
FROM centro c
WHERE c.cif = 'B00000001'
  AND NOT EXISTS (
      SELECT 1 FROM perro p WHERE p.centro_id = c.id AND p.nombre = 'Rizo B2B Demo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.dog.b2b', MIN(p.id)
FROM centro c
JOIN perro p ON p.centro_id = c.id AND p.nombre = 'Rizo B2B Demo'
WHERE c.cif = 'B00000001'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.dog.b2b'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria
    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
SELECT c.id, dog.entity_id, 'EN_INSTALACIONES', CURRENT_DATE + 1, '10:00:00',
       'SOLICITADA', 'Solicitud interna de demostración'
FROM centro c
JOIN dev_fixture_registry dog ON dog.fixture_key = 'reception.dog.luna.ana'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.internal.pending'
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria r
      WHERE r.perro_id = dog.entity_id AND r.estado = 'SOLICITADA'
        AND r.notas = 'Solicitud interna de demostración'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.request.internal.pending', MAX(r.id)
FROM solicitud_peluqueria r
JOIN perro p ON p.id = r.perro_id
JOIN centro c ON c.id = p.centro_id
WHERE c.cif = 'B00000000'
  AND r.estado = 'SOLICITADA'
  AND r.notas = 'Solicitud interna de demostración'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.internal.pending'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id)
SELECT request_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry request_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE request_ref.fixture_key = 'grooming.request.internal.pending'
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria_servicio link
      WHERE link.solicitud_id = request_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO solicitud_peluqueria
    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
SELECT c.id, dog.entity_id, 'EN_INSTALACIONES', CURRENT_DATE + 2, '12:30:00',
       'CONFIRMADA', 'Solicitud interna confirmada de demostración'
FROM centro c
JOIN dev_fixture_registry dog ON dog.fixture_key = 'reception.dog.luna.ana'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.internal.confirmed'
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria r
      WHERE r.perro_id = dog.entity_id AND r.estado = 'CONFIRMADA'
        AND r.notas = 'Solicitud interna confirmada de demostración'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.request.internal.confirmed', MAX(r.id)
FROM solicitud_peluqueria r
JOIN perro p ON p.id = r.perro_id
JOIN centro c ON c.id = p.centro_id
WHERE c.cif = 'B00000000'
  AND r.estado = 'CONFIRMADA'
  AND r.notas = 'Solicitud interna confirmada de demostración'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.internal.confirmed'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id)
SELECT request_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry request_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE request_ref.fixture_key = 'grooming.request.internal.confirmed'
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria_servicio link
      WHERE link.solicitud_id = request_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO solicitud_peluqueria
    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
SELECT c.id, dog.entity_id, 'EN_INSTALACIONES', CURRENT_DATE + 3, '09:30:00',
       'COMPLETADA', 'Solicitud B2B completada de demostración'
FROM centro c
JOIN dev_fixture_registry dog ON dog.fixture_key = 'grooming.dog.b2b'
WHERE c.cif = 'B00000001'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.b2b.completed'
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria r
      WHERE r.perro_id = dog.entity_id AND r.estado = 'COMPLETADA'
        AND r.notas = 'Solicitud B2B completada de demostración'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.request.b2b.completed', MAX(r.id)
FROM solicitud_peluqueria r
JOIN perro p ON p.id = r.perro_id
JOIN centro c ON c.id = p.centro_id
WHERE c.cif = 'B00000001'
  AND r.estado = 'COMPLETADA'
  AND r.notas = 'Solicitud B2B completada de demostración'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.b2b.completed'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id)
SELECT request_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry request_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE request_ref.fixture_key = 'grooming.request.b2b.completed'
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria_servicio link
      WHERE link.solicitud_id = request_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO solicitud_peluqueria
    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
SELECT c.id, dog.entity_id, 'EN_INSTALACIONES', CURRENT_DATE + 4, '14:00:00',
       'CANCELADA', 'Solicitud B2B cancelada de demostración'
FROM centro c
JOIN dev_fixture_registry dog ON dog.fixture_key = 'grooming.dog.b2b'
WHERE c.cif = 'B00000001'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.b2b.cancelled'
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria r
      WHERE r.perro_id = dog.entity_id AND r.estado = 'CANCELADA'
        AND r.notas = 'Solicitud B2B cancelada de demostración'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.request.b2b.cancelled', MAX(r.id)
FROM solicitud_peluqueria r
JOIN perro p ON p.id = r.perro_id
JOIN centro c ON c.id = p.centro_id
WHERE c.cif = 'B00000001'
  AND r.estado = 'CANCELADA'
  AND r.notas = 'Solicitud B2B cancelada de demostración'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.b2b.cancelled'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id)
SELECT request_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry request_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE request_ref.fixture_key = 'grooming.request.b2b.cancelled'
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria_servicio link
      WHERE link.solicitud_id = request_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO centro (nombre, tipo, cif, direccion, activo)
SELECT 'Otro centro peluquería Demo', 'PROPIA', 'B00000002', 'Calle Ficticia 3', TRUE
WHERE NOT EXISTS (SELECT 1 FROM centro WHERE cif = 'B00000002');

INSERT INTO servicio_peluqueria (centro_id, nombre, duracion_min, precio, activo)
SELECT c.id, 'Servicio de otro centro Demo', 60, 35.00, TRUE
FROM centro c
WHERE c.cif = 'B00000002'
  AND NOT EXISTS (
      SELECT 1 FROM servicio_peluqueria s
      WHERE s.centro_id = c.id AND s.nombre = 'Servicio de otro centro Demo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.service.other-center', MIN(s.id)
FROM centro c
JOIN servicio_peluqueria s ON s.centro_id = c.id AND s.nombre = 'Servicio de otro centro Demo'
WHERE c.cif = 'B00000002'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.service.other-center'
  )
GROUP BY c.id;

INSERT INTO perro (centro_id, nombre, tamano, activo)
SELECT c.id, 'Lola otro centro Demo', 'MEDIANO', TRUE
FROM centro c
WHERE c.cif = 'B00000002'
  AND NOT EXISTS (
      SELECT 1 FROM perro p WHERE p.centro_id = c.id AND p.nombre = 'Lola otro centro Demo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.dog.other-center', MIN(p.id)
FROM centro c
JOIN perro p ON p.centro_id = c.id AND p.nombre = 'Lola otro centro Demo'
WHERE c.cif = 'B00000002'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.dog.other-center'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria
    (centro_cliente_id, perro_id, modalidad, fecha_solicitada, hora_solicitada, estado, notas)
SELECT c.id, dog.entity_id, 'EN_INSTALACIONES', CURRENT_DATE + 1, '11:00:00',
       'SOLICITADA', 'Solicitud de centro ajeno para probar aislamiento'
FROM centro c
JOIN dev_fixture_registry dog ON dog.fixture_key = 'grooming.dog.other-center'
WHERE c.cif = 'B00000002'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.other-center'
  )
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria r
      WHERE r.perro_id = dog.entity_id AND r.estado = 'SOLICITADA'
        AND r.notas = 'Solicitud de centro ajeno para probar aislamiento'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.request.other-center', MAX(r.id)
FROM solicitud_peluqueria r
JOIN perro p ON p.id = r.perro_id
JOIN centro c ON c.id = p.centro_id
WHERE c.cif = 'B00000002'
  AND r.estado = 'SOLICITADA'
  AND r.notas = 'Solicitud de centro ajeno para probar aislamiento'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.request.other-center'
  )
GROUP BY c.id;

INSERT INTO solicitud_peluqueria_servicio (solicitud_id, servicio_id)
SELECT request_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry request_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.other-center'
WHERE request_ref.fixture_key = 'grooming.request.other-center'
  AND NOT EXISTS (
      SELECT 1 FROM solicitud_peluqueria_servicio link
      WHERE link.solicitud_id = request_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

-- La relación repetida perro/servicio comprueba que las nuevas solicitudes conservan su propio histórico.
SELECT fixture_key, entity_id
FROM dev_fixture_registry
WHERE fixture_key LIKE 'grooming.%'
ORDER BY fixture_key;

-- Citas de ejemplo para validar el buscador el 25/11/2026.
-- Requiere el fixture de recepción y asigna las citas al primer peluquero activo del centro propio.
INSERT INTO cita_peluqueria
    (perro_id, peluquero_id, inicio, duracion_min, lugar, estado)
SELECT dog_ref.entity_id, groomer.id, TIMESTAMP '2026-11-25 09:30:00',
       60, 'EN_INSTALACIONES', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = (
    SELECT MIN(g.id)
    FROM usuarios g
    JOIN rol r ON r.id = g.rol_id
    WHERE g.centro_id = provider.id AND r.nombre = 'PELUQUERO' AND g.activo = TRUE
)
WHERE dog_ref.fixture_key = 'reception.dog.luna.ana'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.internal.luna'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer.id
        AND c.inicio = TIMESTAMP '2026-11-25 09:30:00'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.today.internal.luna', MIN(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ana'
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = c.peluquero_id AND groomer.centro_id = provider.id
WHERE c.perro_id = dog_ref.entity_id
  AND c.inicio = TIMESTAMP '2026-11-25 09:30:00'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.internal.luna'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.appointment.today.internal.luna'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO cita_peluqueria
    (perro_id, peluquero_id, inicio, duracion_min, lugar, estado)
SELECT dog_ref.entity_id, groomer.id, TIMESTAMP '2026-11-25 11:00:00',
       45, 'EN_INSTALACIONES', 'SOLICITADA'
FROM dev_fixture_registry dog_ref
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = (
    SELECT MIN(g.id)
    FROM usuarios g
    JOIN rol r ON r.id = g.rol_id
    WHERE g.centro_id = provider.id AND r.nombre = 'PELUQUERO' AND g.activo = TRUE
)
WHERE dog_ref.fixture_key = 'reception.dog.bruno.ana'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.internal.bruno'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer.id
        AND c.inicio = TIMESTAMP '2026-11-25 11:00:00'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.today.internal.bruno', MIN(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = c.peluquero_id AND groomer.centro_id = provider.id
WHERE c.perro_id = dog_ref.entity_id
  AND c.inicio = TIMESTAMP '2026-11-25 11:00:00'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.internal.bruno'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.appointment.today.internal.bruno'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO cita_peluqueria
    (perro_id, peluquero_id, inicio, duracion_min, lugar, estado)
SELECT dog_ref.entity_id, groomer.id, TIMESTAMP '2026-11-25 13:30:00',
       75, 'DESPLAZADO', 'COMPLETADA'
FROM dev_fixture_registry dog_ref
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = (
    SELECT MIN(g.id)
    FROM usuarios g
    JOIN rol r ON r.id = g.rol_id
    WHERE g.centro_id = provider.id AND r.nombre = 'PELUQUERO' AND g.activo = TRUE
)
WHERE dog_ref.fixture_key = 'grooming.dog.b2b'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.b2b.rizo'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer.id
        AND c.inicio = TIMESTAMP '2026-11-25 13:30:00'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.today.b2b.rizo', MIN(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'grooming.dog.b2b'
JOIN centro provider ON provider.cif = 'B00000000'
JOIN usuarios groomer ON groomer.id = c.peluquero_id AND groomer.centro_id = provider.id
WHERE c.perro_id = dog_ref.entity_id
  AND c.inicio = TIMESTAMP '2026-11-25 13:30:00'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.today.b2b.rizo'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.appointment.today.b2b.rizo'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );
