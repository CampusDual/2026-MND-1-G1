-- Tutores ficticios para probar la gestión desde la interfaz.
-- Ejecutar manualmente solo en la base HSQLDB local (perfil p).
-- No crea usuarios, no actualiza fichas existentes y no se ejecuta al arrancar.

-- Registro auxiliar local para conservar la identidad del perro fixture aunque se edite su nombre.
CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Ana', 'García López', 'DEMO-TUTOR-ANA', 'ana.garcia@example.com', '+1 202 555 0101'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-ANA'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Iñaki', 'Muñoz', 'DEMO-TUTOR-INAKI', 'inaki.munoz@example.com', '+1 202 555 0102'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-INAKI'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'María del Mar', 'O''Neill', 'DEMO-TUTOR-MARIA', 'maria.oneill@example.com', '+1 202 555 0103'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-MARIA'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Alex', 'Martín', 'DEMO-TUTOR-ALEX-1', 'alex.martin.1@example.com', '+1 202 555 0104'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-ALEX-1'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Alex', 'Martín', 'DEMO-TUTOR-ALEX-2', 'alex.martin.2@example.com', '+1 202 555 0105'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-ALEX-2'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Tutor', 'Contacto compartido', 'DEMO-TUTOR-COMPARTIDO',
       'ana.garcia@example.com', '+1 202 555 0101'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-COMPARTIDO'
  );

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Tutor', 'Con perro asociado', 'DEMO-TUTOR-PERRO',
       'tutor.perro@example.com', '+1 202 555 0106'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-PERRO'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutors.dog.editing', MIN(p.id)
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-PERRO'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id
    AND p.nombre = 'Perro de prueba de tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutors.dog.editing')
GROUP BY c.id, t.id;

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT c.id, t.id, 'Perro de prueba de tutor', 'MEDIANO', TRUE
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-PERRO'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutors.dog.editing');

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutors.dog.editing', p.id
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-PERRO'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = 'Perro de prueba de tutor'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (SELECT 1 FROM dev_fixture_registry WHERE fixture_key = 'tutors.dog.editing');

INSERT INTO tutor (centro_id, nombre, apellidos, dni, email, telefono)
SELECT c.id, 'Elena', 'Prueba de edición', 'DEMO-TUTOR-EDITAR',
       'elena.inicial@example.com', '+1 202 555 0107'
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM tutor t WHERE t.centro_id = c.id AND t.dni = 'DEMO-TUTOR-EDITAR'
  );
