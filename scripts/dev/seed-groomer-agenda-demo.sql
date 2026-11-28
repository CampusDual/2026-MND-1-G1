-- Datos locales para consultar agendas de peluqueros. Ejecutar solo en HSQLDB local.
-- Requiere seed-reception-demo.sql y seed-grooming-requests-demo.sql.
CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

-- Las cuentas nuevas reutilizan el hash local de recepción; no se cambian cuentas existentes.
INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT center.id, groomer_role.id, 'Ada', 'Peluquera Demo', 'AGENDA-PELUQ-A',
       'agenda-peluquera-a@guarderiademo.test', 'agenda-peluquera-a', receptionist.password_hash, TRUE
FROM centro center
JOIN rol groomer_role ON groomer_role.nombre = 'PELUQUERO'
JOIN usuarios receptionist ON receptionist.username = 'recepcionista'
WHERE center.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'agenda-peluquera-a');

INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT center.id, groomer_role.id, 'Eva', 'Sin Citas Demo', 'AGENDA-PELUQ-E',
       'agenda-peluquera-vacia@guarderiademo.test', 'agenda-peluquera-vacia',
       receptionist.password_hash, TRUE
FROM centro center
JOIN rol groomer_role ON groomer_role.nombre = 'PELUQUERO'
JOIN usuarios receptionist ON receptionist.username = 'recepcionista'
WHERE center.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'agenda-peluquera-vacia');

INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT center.id, groomer_role.id, 'Noa', 'Otro Centro Demo', 'AGENDA-PELUQ-F',
       'agenda-peluquera-ajena@guarderiademo.test', 'agenda-peluquera-ajena',
       receptionist.password_hash, TRUE
FROM centro center
JOIN rol groomer_role ON groomer_role.nombre = 'PELUQUERO'
JOIN usuarios receptionist ON receptionist.username = 'recepcionista'
WHERE center.cif = 'B00000002'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'agenda-peluquera-ajena');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.groomer.internal', id
FROM usuarios
WHERE username = 'agenda-peluquera-a'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.groomer.internal'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.groomer.empty', id
FROM usuarios
WHERE username = 'agenda-peluquera-vacia'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.groomer.empty'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.groomer.foreign', id
FROM usuarios
WHERE username = 'agenda-peluquera-ajena'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.groomer.foreign'
  );

-- Citas asignadas de ambos orígenes y con estados ya existentes.
INSERT INTO cita_peluqueria (perro_id, peluquero_id, inicio, duracion_min, lugar, estado, observaciones)
SELECT dog_ref.entity_id, groomer_ref.entity_id, TIMESTAMP(CURRENT_DATE + 1, TIME '09:00:00'),
       60, 'EN_INSTALACIONES', 'CONFIRMADA', 'Fixture agenda demo interna confirmada'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE dog_ref.fixture_key = 'reception.dog.luna.ana'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.appointment.internal.confirmed'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer_ref.entity_id
        AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '09:00:00')
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.appointment.internal.confirmed', MAX(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ana'
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE c.perro_id = dog_ref.entity_id
  AND c.peluquero_id = groomer_ref.entity_id
  AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '09:00:00')
  AND c.observaciones = 'Fixture agenda demo interna confirmada'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'grooming.agenda.appointment.internal.confirmed'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.agenda.appointment.internal.confirmed'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO cita_peluqueria (perro_id, peluquero_id, inicio, duracion_min, lugar, estado, observaciones)
SELECT dog_ref.entity_id, groomer_ref.entity_id, TIMESTAMP(CURRENT_DATE + 1, TIME '10:30:00'),
       45, 'EN_INSTALACIONES', 'SOLICITADA', 'Fixture agenda demo interna solicitada'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE dog_ref.fixture_key = 'reception.dog.bruno.ana'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.appointment.internal.requested'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer_ref.entity_id
        AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '10:30:00')
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.appointment.internal.requested', MAX(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE c.perro_id = dog_ref.entity_id
  AND c.peluquero_id = groomer_ref.entity_id
  AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '10:30:00')
  AND c.observaciones = 'Fixture agenda demo interna solicitada'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'grooming.agenda.appointment.internal.requested'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.agenda.appointment.internal.requested'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO cita_peluqueria (perro_id, peluquero_id, inicio, duracion_min, lugar, estado, observaciones)
SELECT dog_ref.entity_id, groomer_ref.entity_id, TIMESTAMP(CURRENT_DATE + 1, TIME '12:00:00'),
       75, 'DESPLAZADO', 'COMPLETADA', 'Fixture agenda demo B2B completada'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE dog_ref.fixture_key = 'grooming.dog.b2b'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.agenda.appointment.b2b.completed'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer_ref.entity_id
        AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '12:00:00')
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.appointment.b2b.completed', MAX(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'grooming.dog.b2b'
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE c.perro_id = dog_ref.entity_id
  AND c.peluquero_id = groomer_ref.entity_id
  AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '12:00:00')
  AND c.observaciones = 'Fixture agenda demo B2B completada'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'grooming.agenda.appointment.b2b.completed'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.agenda.appointment.b2b.completed'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

INSERT INTO cita_peluqueria (perro_id, peluquero_id, inicio, duracion_min, lugar, estado, observaciones)
SELECT dog_ref.entity_id, groomer_ref.entity_id, TIMESTAMP(CURRENT_DATE + 1, TIME '14:00:00'),
       45, 'EN_INSTALACIONES', 'CANCELADA', 'Fixture agenda demo interna cancelada'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE dog_ref.fixture_key = 'reception.dog.kira.belen'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'grooming.agenda.appointment.internal.cancelled'
  )
  AND NOT EXISTS (
      SELECT 1 FROM cita_peluqueria c
      WHERE c.perro_id = dog_ref.entity_id AND c.peluquero_id = groomer_ref.entity_id
        AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '14:00:00')
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.agenda.appointment.internal.cancelled', MAX(c.id)
FROM cita_peluqueria c
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.kira.belen'
JOIN dev_fixture_registry groomer_ref ON groomer_ref.fixture_key = 'grooming.agenda.groomer.internal'
WHERE c.perro_id = dog_ref.entity_id
  AND c.peluquero_id = groomer_ref.entity_id
  AND c.inicio = TIMESTAMP(CURRENT_DATE + 1, TIME '14:00:00')
  AND c.observaciones = 'Fixture agenda demo interna cancelada'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'grooming.agenda.appointment.internal.cancelled'
  )
GROUP BY dog_ref.entity_id;

INSERT INTO cita_servicio (cita_id, servicio_id)
SELECT appointment_ref.entity_id, service_ref.entity_id
FROM dev_fixture_registry appointment_ref
JOIN dev_fixture_registry service_ref ON service_ref.fixture_key = 'grooming.service.bano-demo'
WHERE appointment_ref.fixture_key = 'grooming.agenda.appointment.internal.cancelled'
  AND NOT EXISTS (
      SELECT 1 FROM cita_servicio link
      WHERE link.cita_id = appointment_ref.entity_id AND link.servicio_id = service_ref.entity_id
  );

SELECT fixture_key, entity_id
FROM dev_fixture_registry
WHERE fixture_key LIKE 'grooming.agenda.%'
ORDER BY fixture_key;
