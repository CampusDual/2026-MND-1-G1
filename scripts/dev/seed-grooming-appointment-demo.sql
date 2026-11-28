-- Opciones de prueba para crear citas de peluquería desde recepción.
-- Ejecutar después de seed-reception-demo.sql y seed-grooming-requests-demo.sql.
-- Solo para HSQLDB local; no se ejecuta al arrancar y no borra citas ni datos existentes.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO servicio_peluqueria (centro_id, nombre, duracion_min, precio, activo)
SELECT c.id, 'Servicio inactivo de cita Demo', 45, 0.00, FALSE
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM servicio_peluqueria s
      WHERE s.centro_id = c.id AND s.nombre = 'Servicio inactivo de cita Demo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.service.inactive', MIN(s.id)
FROM centro c
JOIN servicio_peluqueria s
  ON s.centro_id = c.id AND s.nombre = 'Servicio inactivo de cita Demo' AND s.activo = FALSE
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.service.inactive'
  )
GROUP BY c.id;

INSERT INTO centro (nombre, tipo, cif, direccion, activo)
SELECT 'Centro sin opciones de peluquería Demo', 'PROPIA', 'B00000003',
       'Calle Ficticia 4', TRUE
WHERE NOT EXISTS (SELECT 1 FROM centro WHERE cif = 'B00000003');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.center.empty', c.id
FROM centro c
WHERE c.cif = 'B00000003'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.center.empty'
  );

INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT center_row.id, role_row.id, 'Recepción', 'Sin opciones Demo',
       'DEMO-GROOM-EMPTY-01', 'recepcionista-sin-opciones@guarderiademo.test',
       'recepcionista-sin-opciones', source_user.password_hash, TRUE
FROM centro center_row
JOIN rol role_row ON role_row.nombre = 'RECEPCIONISTA'
JOIN usuarios source_user ON source_user.username = 'recepcionista'
WHERE center_row.cif = 'B00000003'
  AND NOT EXISTS (
      SELECT 1 FROM usuarios WHERE username = 'recepcionista-sin-opciones'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'grooming.appointment.user.empty', u.id
FROM usuarios u
WHERE u.username = 'recepcionista-sin-opciones'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'grooming.appointment.user.empty'
  );

-- Se reutiliza el perro B2B del fixture de peluquería; no se precargan citas nuevas.
SELECT fixture_key, entity_id
FROM dev_fixture_registry
WHERE fixture_key IN (
    'grooming.appointment.service.inactive',
    'grooming.appointment.center.empty',
    'grooming.appointment.user.empty',
    'grooming.dog.b2b',
    'grooming.service.bano-demo',
    'reception.dog.luna.ana'
)
ORDER BY fixture_key;
