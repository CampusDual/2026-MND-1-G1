-- Crea cuentas locales para las fichas de tutor existentes y asegura un perro demo para Vanessa.
-- Ejecutar manualmente solo en la BD HSQLDB local (perfil p); nunca en produccion.
-- Las cuentas nuevas usan el hash BCrypt de la cuenta demo "tutor" (contrasena: admin1234).
-- No modifica ni elimina cuentas demo, perros existentes, ni relaciones de reservas.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO usuarios
    (centro_id, rol_id, nombre, apellidos, dni, email, password_hash, foto_url,
     activo, creado_en, username, tutor_id)
SELECT tutor.centro_id, role.id, tutor.nombre, tutor.apellidos, tutor.dni, tutor.email,
       demo_user.password_hash, NULL, TRUE, CURRENT_TIMESTAMP, tutor.email, tutor.id
FROM tutor
JOIN rol role ON role.nombre = 'TUTOR'
JOIN usuarios demo_user ON demo_user.username = 'tutor' AND demo_user.rol_id = role.id
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios linked_user WHERE linked_user.tutor_id = tutor.id
);

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT tutor.centro_id, tutor.id, 'Perro demo de Vanessa', 'MEDIANO', TRUE
FROM tutor
WHERE tutor.dni = '77245608J'
  AND NOT EXISTS (SELECT 1 FROM perro WHERE perro.tutor_id = tutor.id)
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-user-accounts.vanessa-dog'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-user-accounts.vanessa-dog', dog.id
FROM tutor
JOIN perro dog ON dog.tutor_id = tutor.id AND dog.nombre = 'Perro demo de Vanessa'
WHERE tutor.dni = '77245608J'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-user-accounts.vanessa-dog'
  );

-- Resultado de verificacion: cada fila debe mostrar una cuenta Tutor y al menos un perro.
SELECT tutor.id AS tutor_id, tutor.nombre, tutor.apellidos,
       linked_user.username, linked_user.rol_id, COUNT(DISTINCT dog.id) AS perros
FROM tutor
LEFT JOIN usuarios linked_user ON linked_user.tutor_id = tutor.id
LEFT JOIN perro dog ON dog.tutor_id = tutor.id
GROUP BY tutor.id, tutor.nombre, tutor.apellidos, linked_user.username, linked_user.rol_id
ORDER BY tutor.id;

-- Confirma que las cuentas demo siguen presentes con sus roles originales.
SELECT username, rol_id
FROM usuarios
WHERE username IN ('admin', 'recepcionista', 'tutor')
ORDER BY username;
