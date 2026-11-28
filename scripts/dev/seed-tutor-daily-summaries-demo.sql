-- Datos ficticios para consultar resúmenes diarios desde el portal tutor.
-- Ejecutar manualmente solo en HSQLDB local, después de las migraciones y de
-- seed-tutor-portal-demo.sql. No borrar ni sobrescribir datos existentes.

CREATE TABLE IF NOT EXISTS dev_fixture_registry (
    fixture_key VARCHAR(120) PRIMARY KEY,
    entity_id BIGINT NOT NULL
);

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT account.centro_id, account.tutor_id, 'Perro Demo Resúmenes E21', 'MEDIANO', TRUE
FROM usuarios account
WHERE account.username = 'portal-a'
  AND account.tutor_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-a'
  )
  AND NOT EXISTS (
      SELECT 1 FROM perro dog
      WHERE dog.tutor_id = account.tutor_id
        AND dog.nombre = 'Perro Demo Resúmenes E21'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.dog-a', dog.id
FROM perro dog
JOIN usuarios account ON account.tutor_id = dog.tutor_id
WHERE account.username = 'portal-a'
  AND dog.nombre = 'Perro Demo Resúmenes E21'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-a'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT account.centro_id, account.tutor_id, 'Perro Demo Otro Tutor E21', 'PEQUENO', TRUE
FROM usuarios account
WHERE account.username = 'portal-b'
  AND account.tutor_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-b'
  )
  AND NOT EXISTS (
      SELECT 1 FROM perro dog
      WHERE dog.tutor_id = account.tutor_id
        AND dog.nombre = 'Perro Demo Otro Tutor E21'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.dog-b', dog.id
FROM perro dog
JOIN usuarios account ON account.tutor_id = dog.tutor_id
WHERE account.username = 'portal-b'
  AND dog.nombre = 'Perro Demo Otro Tutor E21'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-b'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT account.centro_id, account.tutor_id, 'Perro Demo Sin Resumen E21', 'MEDIANO', TRUE
FROM usuarios account
WHERE account.username = 'portal-c'
  AND account.tutor_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-c'
  )
  AND NOT EXISTS (
      SELECT 1 FROM perro dog
      WHERE dog.tutor_id = account.tutor_id
        AND dog.nombre = 'Perro Demo Sin Resumen E21'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.dog-c', dog.id
FROM perro dog
JOIN usuarios account ON account.tutor_id = dog.tutor_id
WHERE account.username = 'portal-c'
  AND dog.nombre = 'Perro Demo Sin Resumen E21'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.dog-c'
  );

INSERT INTO reserva (perro_id, fecha, modalidad, estado)
SELECT dog.entity_id, CURRENT_DATE - 1, 'DIA_COMPLETO', 'COMPLETADA'
FROM dev_fixture_registry dog
WHERE dog.fixture_key = 'tutor-summary-e21.dog-a'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-seen'
  )
  AND NOT EXISTS (
      SELECT 1 FROM reserva
      WHERE perro_id = dog.entity_id AND fecha = CURRENT_DATE - 1
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.reservation-seen', reservation.id
FROM reserva reservation
JOIN dev_fixture_registry dog ON dog.entity_id = reservation.perro_id
WHERE dog.fixture_key = 'tutor-summary-e21.dog-a'
  AND reservation.fecha = CURRENT_DATE - 1
  AND reservation.estado = 'COMPLETADA'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-seen'
  );

INSERT INTO reserva (perro_id, fecha, modalidad, estado)
SELECT dog.entity_id, CURRENT_DATE, 'DIA_COMPLETO', 'COMPLETADA'
FROM dev_fixture_registry dog
WHERE dog.fixture_key = 'tutor-summary-e21.dog-a'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-unseen'
  )
  AND NOT EXISTS (
      SELECT 1 FROM reserva
      WHERE perro_id = dog.entity_id AND fecha = CURRENT_DATE
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.reservation-unseen', reservation.id
FROM reserva reservation
JOIN dev_fixture_registry dog ON dog.entity_id = reservation.perro_id
WHERE dog.fixture_key = 'tutor-summary-e21.dog-a'
  AND reservation.fecha = CURRENT_DATE
  AND reservation.estado = 'COMPLETADA'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-unseen'
  );

INSERT INTO resumen_diario
    (reserva_id, destinatario, texto_ia, generado_en, centro_id, estado, publicado_en)
SELECT reservation.entity_id, 'TUTOR',
       'Resumen de demostración visto para Perro Demo Resúmenes E21.',
       CURRENT_TIMESTAMP, account.centro_id, 'PUBLICADO', CURRENT_TIMESTAMP
FROM dev_fixture_registry reservation
JOIN dev_fixture_registry dog ON dog.fixture_key = 'tutor-summary-e21.dog-a'
JOIN usuarios account ON account.username = 'portal-a'
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-seen'
  AND NOT EXISTS (
      SELECT 1 FROM resumen_diario
      WHERE reserva_id = reservation.entity_id AND destinatario = 'TUTOR'
  )
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-seen'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.summary-seen', summary.id
FROM resumen_diario summary
JOIN dev_fixture_registry reservation ON reservation.entity_id = summary.reserva_id
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-seen'
  AND summary.destinatario = 'TUTOR'
  AND summary.texto_ia = 'Resumen de demostración visto para Perro Demo Resúmenes E21.'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-seen'
  );

INSERT INTO resumen_diario
    (reserva_id, destinatario, texto_ia, generado_en, centro_id, estado, publicado_en)
SELECT reservation.entity_id, 'TUTOR',
       'Resumen de demostración nuevo para Perro Demo Resúmenes E21.',
       CURRENT_TIMESTAMP, account.centro_id, 'PUBLICADO', CURRENT_TIMESTAMP
FROM dev_fixture_registry reservation
JOIN usuarios account ON account.username = 'portal-a'
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-unseen'
  AND NOT EXISTS (
      SELECT 1 FROM resumen_diario
      WHERE reserva_id = reservation.entity_id AND destinatario = 'TUTOR'
  )
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-unseen'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.summary-unseen', summary.id
FROM resumen_diario summary
JOIN dev_fixture_registry reservation ON reservation.entity_id = summary.reserva_id
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-unseen'
  AND summary.destinatario = 'TUTOR'
  AND summary.texto_ia = 'Resumen de demostración nuevo para Perro Demo Resúmenes E21.'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-unseen'
  );

INSERT INTO reserva (perro_id, fecha, modalidad, estado)
SELECT dog.entity_id, CURRENT_DATE - 1, 'DIA_COMPLETO', 'COMPLETADA'
FROM dev_fixture_registry dog
WHERE dog.fixture_key = 'tutor-summary-e21.dog-b'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-other'
  )
  AND NOT EXISTS (
      SELECT 1 FROM reserva
      WHERE perro_id = dog.entity_id AND fecha = CURRENT_DATE - 1
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.reservation-other', reservation.id
FROM reserva reservation
JOIN dev_fixture_registry dog ON dog.entity_id = reservation.perro_id
WHERE dog.fixture_key = 'tutor-summary-e21.dog-b'
  AND reservation.fecha = CURRENT_DATE - 1
  AND reservation.estado = 'COMPLETADA'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.reservation-other'
  );

INSERT INTO resumen_diario
    (reserva_id, destinatario, texto_ia, generado_en, centro_id, estado, publicado_en)
SELECT reservation.entity_id, 'TUTOR',
       'Resumen publicado privado de Perro Demo Otro Tutor E21.',
       CURRENT_TIMESTAMP, account.centro_id, 'PUBLICADO', CURRENT_TIMESTAMP
FROM dev_fixture_registry reservation
JOIN usuarios account ON account.username = 'portal-b'
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-other'
  AND NOT EXISTS (
      SELECT 1 FROM resumen_diario
      WHERE reserva_id = reservation.entity_id AND destinatario = 'TUTOR'
  )
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-other'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'tutor-summary-e21.summary-other', summary.id
FROM resumen_diario summary
JOIN dev_fixture_registry reservation ON reservation.entity_id = summary.reserva_id
WHERE reservation.fixture_key = 'tutor-summary-e21.reservation-other'
  AND summary.destinatario = 'TUTOR'
  AND summary.texto_ia = 'Resumen publicado privado de Perro Demo Otro Tutor E21.'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry
      WHERE fixture_key = 'tutor-summary-e21.summary-other'
  );

INSERT INTO resumen_diario_lectura (resumen_id, tutor_id, visto_en)
SELECT summary.entity_id, account.tutor_id, CURRENT_TIMESTAMP
FROM dev_fixture_registry summary
JOIN usuarios account ON account.username = 'portal-a'
WHERE summary.fixture_key = 'tutor-summary-e21.summary-seen'
  AND NOT EXISTS (
      SELECT 1 FROM resumen_diario_lectura reading
      WHERE reading.resumen_id = summary.entity_id
        AND reading.tutor_id = account.tutor_id
  );

SELECT account.username, dog.nombre AS perro, summary.reserva_id, summary.estado,
       reading.visto_en IS NOT NULL AS visto_por_tutor
FROM dev_fixture_registry summary_ref
JOIN resumen_diario summary ON summary.id = summary_ref.entity_id
JOIN reserva reservation ON reservation.id = summary.reserva_id
JOIN perro dog ON dog.id = reservation.perro_id
JOIN usuarios account ON account.tutor_id = dog.tutor_id
LEFT JOIN resumen_diario_lectura reading
       ON reading.resumen_id = summary.id AND reading.tutor_id = account.tutor_id
WHERE summary_ref.fixture_key LIKE 'tutor-summary-e21.summary-%'
ORDER BY reservation.fecha DESC;
