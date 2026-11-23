-- Datos ficticios locales del portal cuidador. Ejecutar manualmente después de V13 y seed-reception-demo.sql.
-- No cambia contraseñas existentes ni elimina datos.

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'CUIDADOR', 'Demo A', 'DEMO-CUIDADOR-A',
       'cuidador-a@guarderiademo.test', 'cuidador-a', demo.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'CUIDADOR'
JOIN usuarios demo ON demo.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'cuidador-a');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'CUIDADOR', 'Demo B', 'DEMO-CUIDADOR-B',
       'cuidador-b@guarderiademo.test', 'cuidador-b', demo.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'CUIDADOR'
JOIN usuarios demo ON demo.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'cuidador-b');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'CUIDADOR', 'Demo C', 'DEMO-CUIDADOR-C',
       'cuidador-c@guarderiademo.test', 'cuidador-c', demo.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'CUIDADOR'
JOIN usuarios demo ON demo.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'cuidador-c');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'CUIDADOR', 'Demo D', 'DEMO-CUIDADOR-D',
       'cuidador-d@guarderiademo.test', 'cuidador-d', demo.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'CUIDADOR'
JOIN usuarios demo ON demo.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'cuidador-d');

UPDATE grupo
SET cuidador_id = (SELECT id FROM usuarios WHERE username = 'cuidador-a')
WHERE centro_id = (SELECT id FROM centro WHERE cif = 'B00000000')
  AND nombre = 'Recepcion Demo';

INSERT INTO grupo (centro_id, nombre, sala, capacidad_max, cuidador_id)
SELECT c.id, 'Caregiver Demo B', 'Sala B', 20,
       (SELECT id FROM usuarios WHERE username = 'cuidador-b')
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM grupo g WHERE g.centro_id = c.id AND g.nombre = 'Caregiver Demo B'
  );

UPDATE reserva
SET grupo_id = (SELECT g.id FROM grupo g JOIN centro c ON c.id = g.centro_id
                WHERE c.cif = 'B00000000' AND g.nombre = 'Caregiver Demo B')
WHERE fecha = CURRENT_DATE
  AND perro_id IN (
      SELECT p.id FROM perro p
      JOIN dev_fixture_registry r ON r.entity_id = p.id
      WHERE r.fixture_key = 'reception.dog.kira.belen'
  );

INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora, estado_mascota)
SELECT r.id, u.id, 'ENTRADA', CURRENT_TIMESTAMP, 'Llegó en buen estado'
FROM reserva r
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ana'
JOIN perro p ON p.id = r.perro_id AND p.id = dog_ref.entity_id
JOIN usuarios u ON u.username = 'recepcionista'
WHERE r.fecha = CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = r.id AND cr.tipo = 'ENTRADA'
  );

INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora, estado_mascota)
SELECT r.id, u.id, 'ENTRADA', CURRENT_TIMESTAMP, 'Llegó en buen estado'
FROM reserva r
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.kira.belen'
JOIN perro p ON p.id = r.perro_id AND p.id = dog_ref.entity_id
JOIN usuarios u ON u.username = 'recepcionista'
WHERE r.fecha = CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = r.id AND cr.tipo = 'ENTRADA'
  );

UPDATE reserva
SET estado = 'EN_CURSO'
WHERE fecha = CURRENT_DATE
  AND id IN (
      SELECT r.id
      FROM reserva r
      JOIN dev_fixture_registry dog_ref ON dog_ref.entity_id = r.perro_id
      WHERE dog_ref.fixture_key IN (
          'reception.dog.bruno.ana',
          'reception.dog.luna.ana',
          'reception.dog.kira.belen'
      )
  )
  AND EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = reserva.id AND cr.tipo = 'ENTRADA'
  );

UPDATE perro
SET temperamento = 'Sociable, reservado con perros desconocidos',
    pautas_alimentacion = 'Dos raciones húmedas al día; sin cambios bruscos'
WHERE id IN (
    SELECT entity_id FROM dev_fixture_registry
    WHERE fixture_key = 'reception.dog.luna.ana'
);

INSERT INTO alergia (perro_id, descripcion, declarada_por_tutor)
SELECT entity_id, 'Polen', FALSE
FROM dev_fixture_registry
WHERE fixture_key = 'reception.dog.luna.ana'
  AND NOT EXISTS (
      SELECT 1 FROM alergia a
      WHERE a.perro_id = dev_fixture_registry.entity_id
        AND a.descripcion = 'Polen'
  );

INSERT INTO medicacion (perro_id, medicamento, dosis, pauta, declarada_por_tutor)
SELECT entity_id, 'Antihistamínico', '1 comprimido', 'Administrar según indicación veterinaria',
       FALSE
FROM dev_fixture_registry
WHERE fixture_key = 'reception.dog.luna.ana'
  AND NOT EXISTS (
      SELECT 1 FROM medicacion m
      WHERE m.perro_id = dev_fixture_registry.entity_id
        AND m.medicamento = 'Antihistamínico'
  );
