ALTER TABLE reserva DROP CONSTRAINT uq_reserva_perro_fecha;

ALTER TABLE reserva DROP CONSTRAINT ck_reserva_modalidad;

ALTER TABLE reserva
    ADD CONSTRAINT ck_reserva_modalidad
        CHECK (modalidad IN ('DIA_COMPLETO', 'MEDIA_JORNADA', 'MANANA', 'TARDE'));

CREATE INDEX ix_reserva_grupo_fecha_estado_modalidad
    ON reserva (grupo_id, fecha, estado, modalidad);

CREATE INDEX ix_reserva_perro_fecha_estado
    ON reserva (perro_id, fecha, estado);
