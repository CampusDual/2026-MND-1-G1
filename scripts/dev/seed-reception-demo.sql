-- Datos ficticios para recepción. Ejecutar manualmente solo en la base HSQLDB local (perfil p).
-- No se ejecuta al arrancar la aplicación y no borra ni modifica datos existentes.
-- D corresponde a CURRENT_DATE en el servidor HSQLDB, que debe usar Europe/Madrid.

-- Registro auxiliar local para conservar la identidad de fixtures aunque se editen sus nombres.
CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO centro (nombre, tipo, cif, direccion, codigo_postal, localidad, telefono, email, activo)
SELECT 'Guarderia Cliente Demo', 'CLIENTE', 'B00000001', 'Calle Ficticia 2', '15002',
       'A Coruna', '981000002', 'cliente@guarderiademo.test', TRUE
FROM centro propio
WHERE propio.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM centro WHERE cif = 'B00000001');

INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Gerente', 'Demo', 'DEMO-REC-GER', 'gerente@guarderiademo.test',
       'gerente', admin.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'GERENTE'
JOIN usuarios admin ON admin.username = 'admin'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'gerente');

INSERT INTO usuarios (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Cliente', 'B2B Demo', 'DEMO-B2B-001', 'cliente-b2b@guarderiademo.test',
       'cliente-b2b', tutor.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'TUTOR'
JOIN usuarios tutor ON tutor.username = 'tutor'
WHERE c.cif = 'B00000001'
  AND NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'cliente-b2b');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Ana', 'Núñez Demo', 'DEMO-REC-01', 'ana.demo@guarderiademo.test', '981000101'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor WHERE centro_id = c.id AND dni = 'DEMO-REC-01');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Belén', 'López Demo', 'DEMO-REC-02', 'belen.demo@guarderiademo.test', '981000102'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor WHERE centro_id = c.id AND dni = 'DEMO-REC-02');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'César', 'Vázquez Demo', 'DEMO-REC-03', 'cesar.demo@guarderiademo.test', '981000103'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor WHERE centro_id = c.id AND dni = 'DEMO-REC-03');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Inés', 'Peña Demo', 'DEMO-REC-04', 'ines.demo@guarderiademo.test', '981000104'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor WHERE centro_id = c.id AND dni = 'DEMO-REC-04');

INSERT INTO persona_autorizada (tutor_id, nombre, telefono)
SELECT t.id, 'María Autorizada Demo', '981000199'
FROM tutor t
JOIN centro c ON c.id = t.centro_id
WHERE c.cif = 'B00000000'
  AND t.dni = 'DEMO-REC-01'
  AND NOT EXISTS (
      SELECT 1 FROM persona_autorizada pa
      WHERE pa.tutor_id = t.id
        AND pa.nombre = 'María Autorizada Demo'
        AND pa.telefono = '981000199'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ana', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Luna'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana')
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ana.duplicate', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id
    AND p.nombre = 'Luna' AND p.raza = 'Caniche' AND p.tamano = 'PEQUENO'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana.duplicate'
  )
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.bruno.ana', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Bruno'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.bruno.ana')
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.kira.belen', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-02'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Kira'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.kira.belen')
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.coco.cesar', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-03'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Coco'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.coco.cesar')
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.nala.ines', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Nala'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.nala.ines')
GROUP BY c.id, t.id;

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ines', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Luna'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ines')
GROUP BY c.id, t.id;

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Luna', 'Mestiza', 'MEDIANO', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ana', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Luna'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Luna', 'Caniche', 'PEQUENO', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana.duplicate'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ana.duplicate', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id
    AND p.nombre = 'Luna' AND p.raza = 'Caniche' AND p.tamano = 'PEQUENO'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ana.duplicate'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Bruno', 'Labrador', 'GRANDE', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.bruno.ana');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.bruno.ana', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Bruno'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.bruno.ana'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Kira', 'Border Collie', 'MEDIANO', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-02'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.kira.belen');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.kira.belen', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-02'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Kira'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.kira.belen'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Coco', NULL, 'PEQUENO', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-03'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.coco.cesar');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.coco.cesar', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-03'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Coco'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.coco.cesar'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Nala', 'Galgo', 'GRANDE', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.nala.ines');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.nala.ines', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Nala'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.nala.ines'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, raza, tamano, activo)
SELECT c.id, t.id, 'Luna', 'Caniche', 'PEQUENO', TRUE
FROM centro c JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ines');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.dog.luna.ines', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Luna'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.dog.luna.ines'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.group.main', MIN(g.id)
FROM centro c
JOIN grupo g ON g.centro_id = c.id AND g.nombre = 'Recepcion Demo'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.group.main')
GROUP BY c.id;

INSERT INTO grupo (centro_id, nombre, capacidad_max)
SELECT c.id, 'Recepcion Demo', 20
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.group.main');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'reception.group.main', g.id
FROM centro c
JOIN grupo g ON g.centro_id = c.id AND g.nombre = 'Recepcion Demo'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'reception.group.main');

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'DIA_COMPLETO', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ana'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'MEDIA_JORNADA', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'MEDIA_JORNADA', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-02'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.kira.belen'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'DIA_COMPLETO', 'COMPLETADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-03'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.coco.cesar'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'DIA_COMPLETO', 'CANCELADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.nala.ines'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE, 'DIA_COMPLETO', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-04'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ines'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE - 1, 'MEDIA_JORNADA', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-02'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.kira.belen'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE - 1);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE + 1, 'DIA_COMPLETO', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.luna.ana'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE + 1);

INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT p.id, g.id, CURRENT_DATE + 1, 'MEDIA_JORNADA', 'CONFIRMADA'
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
JOIN perro p ON p.id = dog_ref.entity_id AND p.centro_id = c.id AND p.tutor_id = t.id
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'reception.group.main'
JOIN grupo g ON g.id = group_ref.entity_id AND g.centro_id = c.id
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM reserva WHERE perro_id = p.id AND fecha = CURRENT_DATE + 1);

INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora, estado_mascota, observaciones)
SELECT r.id, u.id, 'ENTRADA', CURRENT_TIMESTAMP, 'Llegó con buen estado general',
       'Arnés rojo; bolsa azul; recipiente de comida'
FROM reserva r
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
JOIN perro p ON p.id = r.perro_id AND p.id = dog_ref.entity_id
JOIN tutor t ON t.id = p.tutor_id AND t.dni = 'DEMO-REC-01'
JOIN usuarios u ON u.username = 'recepcionista'
WHERE r.fecha = CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = r.id AND cr.tipo = 'ENTRADA' AND CAST(cr.fecha_hora AS DATE) = CURRENT_DATE
  );

UPDATE reserva
SET estado = 'EN_CURSO'
WHERE fecha = CURRENT_DATE
  AND estado = 'CONFIRMADA'
  AND id IN (
      SELECT r.id
      FROM reserva r
      JOIN perro p ON p.id = r.perro_id
      JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.bruno.ana'
      WHERE p.id = dog_ref.entity_id
  )
  AND EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = reserva.id AND cr.tipo = 'ENTRADA'
  );

INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora, estado_mascota)
SELECT r.id, u.id, 'ENTRADA', CURRENT_TIMESTAMP, 'Llegó con buen estado general'
FROM reserva r
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.coco.cesar'
JOIN perro p ON p.id = r.perro_id AND p.id = dog_ref.entity_id
JOIN tutor t ON t.id = p.tutor_id AND t.dni = 'DEMO-REC-03'
JOIN usuarios u ON u.username = 'recepcionista'
WHERE r.fecha = CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = r.id AND cr.tipo = 'ENTRADA' AND CAST(cr.fecha_hora AS DATE) = CURRENT_DATE
  );

INSERT INTO check_registro (reserva_id, usuario_id, tipo, fecha_hora)
SELECT r.id, u.id, 'SALIDA', CURRENT_TIMESTAMP
FROM reserva r
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = 'reception.dog.coco.cesar'
JOIN perro p ON p.id = r.perro_id AND p.id = dog_ref.entity_id
JOIN tutor t ON t.id = p.tutor_id AND t.dni = 'DEMO-REC-03'
JOIN usuarios u ON u.username = 'recepcionista'
WHERE r.fecha = CURRENT_DATE
  AND EXISTS (SELECT 1 FROM check_registro cr WHERE cr.reserva_id = r.id AND cr.tipo = 'ENTRADA')
  AND NOT EXISTS (
      SELECT 1 FROM check_registro cr
      WHERE cr.reserva_id = r.id AND cr.tipo = 'SALIDA' AND CAST(cr.fecha_hora AS DATE) = CURRENT_DATE
  );
