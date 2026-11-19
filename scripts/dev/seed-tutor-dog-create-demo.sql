-- Personas autorizadas ficticias para probar el alta del portal tutor.
-- Ejecutar manualmente solo en HSQLDB local (perfil p), despues del fixture del portal.
-- No crea perros, no cambia autorizaciones existentes ni modifica salidas historicas.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO persona_autorizada (tutor_id, nombre, telefono)
SELECT tutor.id, 'Autorizada Portal A', '5550301'
FROM tutor
JOIN usuarios account ON account.tutor_id = tutor.id
WHERE account.username = 'portal-a'
  AND NOT EXISTS (
      SELECT 1 FROM persona_autorizada person
      WHERE person.tutor_id = tutor.id
        AND person.nombre = 'Autorizada Portal A'
        AND person.telefono = '5550301'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-dog-create.authorized-a', person.id
FROM persona_autorizada person
JOIN tutor ON tutor.id = person.tutor_id
JOIN usuarios account ON account.tutor_id = tutor.id
WHERE account.username = 'portal-a'
  AND person.nombre = 'Autorizada Portal A'
  AND person.telefono = '5550301'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-dog-create.authorized-a'
  );

INSERT INTO persona_autorizada (tutor_id, nombre, telefono)
SELECT tutor.id, 'Autorizado Portal B', '5550302'
FROM tutor
JOIN usuarios account ON account.tutor_id = tutor.id
WHERE account.username = 'portal-b'
  AND NOT EXISTS (
      SELECT 1 FROM persona_autorizada person
      WHERE person.tutor_id = tutor.id
        AND person.nombre = 'Autorizado Portal B'
        AND person.telefono = '5550302'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-dog-create.authorized-b', person.id
FROM persona_autorizada person
JOIN tutor ON tutor.id = person.tutor_id
JOIN usuarios account ON account.tutor_id = tutor.id
WHERE account.username = 'portal-b'
  AND person.nombre = 'Autorizado Portal B'
  AND person.telefono = '5550302'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-dog-create.authorized-b'
  );

SELECT account.username, person.nombre, person.telefono
FROM usuarios account
JOIN tutor ON tutor.id = account.tutor_id
JOIN persona_autorizada person ON person.tutor_id = tutor.id
WHERE account.username IN ('portal-a', 'portal-b')
ORDER BY account.username, person.nombre;
