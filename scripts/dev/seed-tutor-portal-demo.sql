-- Cuentas y perros ficticios para la consulta del portal tutor.
-- Ejecutar manualmente solo en HSQLDB local (perfil p); nunca en producción.
-- No elimina datos ni modifica fichas o contraseñas existentes.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

-- Perfiles ficticios vinculados explícitamente por DNI de fixture.
INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Ana', 'Portal Demo', 'DEMO-PORTAL-TUTOR-A', 'portal.a@example.test', '5550201'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-A');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Bruno', 'Portal Demo', 'DEMO-PORTAL-TUTOR-B', 'portal.b@example.test', '5550202'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-B');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Carla', 'Portal Demo', 'DEMO-PORTAL-TUTOR-C', 'portal.c@example.test', '5550203'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-C');

-- Las cuentas nuevas reutilizan el hash BCrypt ya sembrado para la cuenta tutor.
-- No se almacena una contraseña en texto plano ni se altera la cuenta origen.
INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Tutor Portal A', 'Demo', 'PORTAL-USER-A', 'portal-a@guarderiademo.test',
       'portal-a', source.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'TUTOR'
JOIN usuarios source ON source.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username = 'portal-a');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Tutor Portal B', 'Demo', 'PORTAL-USER-B', 'portal-b@guarderiademo.test',
       'portal-b', source.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'TUTOR'
JOIN usuarios source ON source.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username = 'portal-b');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Tutor Portal C', 'Demo', 'PORTAL-USER-C', 'portal-c@guarderiademo.test',
       'portal-c', source.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'TUTOR'
JOIN usuarios source ON source.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username = 'portal-c');

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, username, password_hash, activo)
SELECT c.id, r.id, 'Tutor Portal D', 'Sin vínculo', 'PORTAL-USER-D', 'portal-d@guarderiademo.test',
       'portal-d', source.password_hash, TRUE
FROM centro c
JOIN rol r ON r.nombre = 'TUTOR'
JOIN usuarios source ON source.username = 'tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.username = 'portal-d');

-- El vínculo cuenta-ficha se basa en las claves estables del fixture, nunca en email o nombre.
UPDATE usuarios
SET tutor_id = (SELECT t.id FROM tutor t
                WHERE t.centro_id = usuarios.centro_id AND t.dni = 'DEMO-PORTAL-TUTOR-A')
WHERE username = 'portal-a' AND tutor_id IS NULL;

UPDATE usuarios
SET tutor_id = (SELECT t.id FROM tutor t
                WHERE t.centro_id = usuarios.centro_id AND t.dni = 'DEMO-PORTAL-TUTOR-B')
WHERE username = 'portal-b' AND tutor_id IS NULL;

UPDATE usuarios
SET tutor_id = (SELECT t.id FROM tutor t
                WHERE t.centro_id = usuarios.centro_id AND t.dni = 'DEMO-PORTAL-TUTOR-C')
WHERE username = 'portal-c' AND tutor_id IS NULL;

-- Perros del modelo canónico, vinculados igual que los perros dados de alta en recepción.
INSERT INTO perro
    (centro_id, tutor_id, nombre, raza, fecha_nacimiento, tamano, temperamento,
     pautas_alimentacion, foto_url, activo)
SELECT c.id, t.id, 'Luna', 'Labrador', DATE '2021-04-12', 'GRANDE',
       'Tranquila y sociable con personas.',
       'Pienso seco para adulto, 250 g por toma, dos tomas al día.',
       'portal-dog-a1.png', TRUE
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-A'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-a1');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-portal.dog-a1', dog.id
FROM perro dog
JOIN tutor t ON t.id = dog.tutor_id
WHERE t.dni = 'DEMO-PORTAL-TUTOR-A' AND dog.nombre = 'Luna'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-a1');

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT c.id, t.id, 'Milo', 'MEDIANO', TRUE
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-A'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-a2');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-portal.dog-a2', dog.id
FROM perro dog
JOIN tutor t ON t.id = dog.tutor_id
WHERE t.dni = 'DEMO-PORTAL-TUTOR-A' AND dog.nombre = 'Milo'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-a2');

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT c.id, t.id, 'Luna', 'PEQUENO', TRUE
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-PORTAL-TUTOR-B'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-b1');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-portal.dog-b1', dog.id
FROM perro dog
JOIN tutor t ON t.id = dog.tutor_id
WHERE t.dni = 'DEMO-PORTAL-TUTOR-B' AND dog.nombre = 'Luna'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.dog-b1');

-- Alergia y pauta representativas; no se registran fechas ni dosis administradas.
INSERT INTO alergia (perro_id, descripcion)
SELECT registry.entity_id, 'Reacción conocida al pollo; evitar alimentos que lo contengan.'
FROM dev_fixture_registry registry
WHERE registry.fixture_key = 'tutor-portal.dog-a1'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.allergy-a1'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-portal.allergy-a1', allergy.id
FROM alergia allergy
JOIN dev_fixture_registry dog ON dog.fixture_key = 'tutor-portal.dog-a1'
WHERE allergy.perro_id = dog.entity_id
  AND allergy.descripcion = 'Reacción conocida al pollo; evitar alimentos que lo contengan.'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.allergy-a1');

INSERT INTO medicacion (perro_id, medicamento, dosis, pauta)
SELECT registry.entity_id, 'Suplemento de prueba', '1 comprimido',
       'Administrar por vía oral una vez al día con la comida.'
FROM dev_fixture_registry registry
WHERE registry.fixture_key = 'tutor-portal.dog-a1'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.medication-a1'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-portal.medication-a1', medication.id
FROM medicacion medication
JOIN dev_fixture_registry dog ON dog.fixture_key = 'tutor-portal.dog-a1'
WHERE medication.perro_id = dog.entity_id
  AND medication.medicamento = 'Suplemento de prueba'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutor-portal.medication-a1');

-- La incompatibilidad es información interna y no debe aparecer en respuestas del portal.
INSERT INTO incompatibilidad (perro_a_id, perro_b_id, motivo)
SELECT LEAST(firstDog.entity_id, secondDog.entity_id),
       GREATEST(firstDog.entity_id, secondDog.entity_id),
       'Nota interna de fixture: comprobar que no se publica al tutor.'
FROM dev_fixture_registry firstDog
JOIN dev_fixture_registry secondDog
  ON firstDog.fixture_key = 'tutor-portal.dog-a1'
 AND secondDog.fixture_key = 'tutor-portal.dog-a2'
WHERE NOT EXISTS (
    SELECT 1 FROM incompatibilidad conflict
    WHERE conflict.perro_a_id = LEAST(firstDog.entity_id, secondDog.entity_id)
      AND conflict.perro_b_id = GREATEST(firstDog.entity_id, secondDog.entity_id)
);
