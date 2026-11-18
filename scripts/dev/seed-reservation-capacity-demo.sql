-- Escenarios de aforo. Ejecutar manualmente solo en HSQLDB local (perfil p).
-- Requiere centro B00000000, tutor DEMO-REC-01 y la tabla dev_fixture_registry.
-- No se ejecuta al arrancar, no borra filas y conserva IDs/fechas al repetirse.

CREATE TABLE IF NOT EXISTS dev_booking_fixture_dates (
    fixture_key VARCHAR(40) PRIMARY KEY,
    fixture_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS dev_booking_fixture_dogs (
    fixture_key VARCHAR(80) PRIMARY KEY,
    dog_name VARCHAR(120) NOT NULL UNIQUE
);

INSERT INTO dev_booking_fixture_dates (fixture_key, fixture_date)
SELECT 'booking.date.' || dates.fixture_key,
       DATEADD('DAY',
               CASE WHEN DAYOFWEEK(CURRENT_DATE) = 1 THEN 1
                    WHEN DAYOFWEEK(CURRENT_DATE) = 2 THEN 0
                    ELSE 9 - DAYOFWEEK(CURRENT_DATE)
               END + dates.day_offset,
               CURRENT_DATE)
FROM (VALUES
    ('F1', 0), ('F2', 1), ('F3', 2), ('F4', 3), ('F5', 4),
    ('F6', 5), ('F7', 7), ('F8', 8), ('F9', 9)
) dates(fixture_key, day_offset)
WHERE NOT EXISTS (
      SELECT 1
      FROM dev_booking_fixture_dates existing
      WHERE existing.fixture_key = 'booking.date.' || dates.fixture_key
  );

INSERT INTO dev_booking_fixture_dogs (fixture_key, dog_name)
SELECT 'booking.dog.' || dogs.dog_name, dogs.dog_name
FROM (VALUES
    ('F1-Perro'),
    ('F2-Manana-1'), ('F2-Manana-2'), ('F2-Manana-3'),
    ('F3-Tarde-1'), ('F3-Tarde-2'), ('F3-Tarde-3'),
    ('F4-Completo-1'), ('F4-Completo-2'), ('F4-Completo-3'),
    ('F5-Completo'), ('F5-Manana'), ('F5-Tarde'),
    ('F6-Completo'), ('F6-Cancelada-1'), ('F6-Cancelada-2'),
    ('F7-Duplicado'), ('F8-ReReserva'),
    ('F9-Manana-1'), ('F9-Manana-2'), ('F9-Competidor-1'), ('F9-Competidor-2')
) dogs(dog_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM dev_booking_fixture_dogs existing
    WHERE existing.fixture_key = 'booking.dog.' || dogs.dog_name
);

INSERT INTO grupo (centro_id, nombre, capacidad_max)
SELECT c.id, 'Reserva Demo Aforo', 3
FROM centro c
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry ref
      WHERE ref.fixture_key = 'booking.group.capacity'
  )
  AND NOT EXISTS (
      SELECT 1 FROM grupo g
      WHERE g.centro_id = c.id AND g.nombre = 'Reserva Demo Aforo'
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT 'booking.group.capacity', g.id
FROM centro c
JOIN grupo g ON g.centro_id = c.id AND g.nombre = 'Reserva Demo Aforo'
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry ref
      WHERE ref.fixture_key = 'booking.group.capacity'
  );

INSERT INTO perro (centro_id, tutor_id, nombre, tamano, activo)
SELECT c.id, t.id, dogs.dog_name, 'MEDIANO', TRUE
FROM centro c
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
CROSS JOIN dev_booking_fixture_dogs dogs
WHERE c.cif = 'B00000000'
  AND NOT EXISTS (
      SELECT 1 FROM dev_fixture_registry ref
      WHERE ref.fixture_key = dogs.fixture_key
  );

INSERT INTO dev_fixture_registry (fixture_key, entity_id)
SELECT dogs.fixture_key, p.id
FROM dev_booking_fixture_dogs dogs
JOIN centro c ON c.cif = 'B00000000'
JOIN tutor t ON t.centro_id = c.id AND t.dni = 'DEMO-REC-01'
JOIN perro p ON p.centro_id = c.id AND p.tutor_id = t.id AND p.nombre = dogs.dog_name
WHERE NOT EXISTS (
    SELECT 1 FROM dev_fixture_registry ref
    WHERE ref.fixture_key = dogs.fixture_key
);

-- F2: mañana completa, tarde disponible.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, fixture_date, 'MANANA', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F2'
WHERE dog_ref.fixture_key IN ('booking.dog.F2-Manana-1', 'booking.dog.F2-Manana-2', 'booking.dog.F2-Manana-3')
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

-- F3: tarde completa, mañana disponible.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, fixture_date, 'TARDE', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F3'
WHERE dog_ref.fixture_key IN ('booking.dog.F3-Tarde-1', 'booking.dog.F3-Tarde-2', 'booking.dog.F3-Tarde-3')
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

-- F4: día completo llena ambas franjas.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, fixture_date, 'DIA_COMPLETO', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F4'
WHERE dog_ref.fixture_key IN ('booking.dog.F4-Completo-1', 'booking.dog.F4-Completo-2', 'booking.dog.F4-Completo-3')
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

-- F5: ocupación mixta: un día completo, una mañana y una tarde.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, dates.fixture_date, bookings.modality, 'CONFIRMADA'
FROM (VALUES
    ('booking.dog.F5-Completo', 'DIA_COMPLETO'),
    ('booking.dog.F5-Manana', 'MANANA'),
    ('booking.dog.F5-Tarde', 'TARDE')
) bookings(fixture_key, modality)
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = bookings.fixture_key
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F5'
WHERE NOT EXISTS (
    SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
);

-- F6: una reserva activa y dos canceladas.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, dates.fixture_date, bookings.modality, bookings.status
FROM (VALUES
    ('booking.dog.F6-Completo', 'DIA_COMPLETO', 'CONFIRMADA'),
    ('booking.dog.F6-Cancelada-1', 'DIA_COMPLETO', 'CANCELADA'),
    ('booking.dog.F6-Cancelada-2', 'DIA_COMPLETO', 'CANCELADA')
) bookings(fixture_key, modality, status)
JOIN dev_fixture_registry dog_ref ON dog_ref.fixture_key = bookings.fixture_key
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F6'
WHERE NOT EXISTS (
    SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
);

-- F7: una reserva activa, se rechaza una segunda reserva del mismo perro.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, dates.fixture_date, 'MANANA', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F7'
WHERE dog_ref.fixture_key = 'booking.dog.F7-Duplicado'
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

-- F8: solo histórico cancelado, se permite crear otra reserva sin sobrescribirlo.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, dates.fixture_date, 'TARDE', 'CANCELADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F8'
WHERE dog_ref.fixture_key = 'booking.dog.F8-ReReserva'
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

-- F9: queda una plaza de mañana, los dos últimos perros compiten por ella.
INSERT INTO reserva (perro_id, grupo_id, fecha, modalidad, estado)
SELECT dog_ref.entity_id, group_ref.entity_id, fixture_date, 'MANANA', 'CONFIRMADA'
FROM dev_fixture_registry dog_ref
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN dev_booking_fixture_dates dates ON dates.fixture_key = 'booking.date.F9'
WHERE dog_ref.fixture_key IN ('booking.dog.F9-Manana-1', 'booking.dog.F9-Manana-2')
  AND NOT EXISTS (
      SELECT 1 FROM reserva r WHERE r.perro_id = dog_ref.entity_id AND r.fecha = dates.fixture_date
  );

SELECT dates.fixture_key, dates.fixture_date, g.capacidad_max
FROM dev_booking_fixture_dates dates
JOIN dev_fixture_registry group_ref ON group_ref.fixture_key = 'booking.group.capacity'
JOIN grupo g ON g.id = group_ref.entity_id
ORDER BY dates.fixture_key;
