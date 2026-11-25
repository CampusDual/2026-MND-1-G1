ALTER TABLE solicitud_peluqueria
    DROP CONSTRAINT ck_solicitud_peluqueria_estado;

ALTER TABLE solicitud_peluqueria
    ADD CONSTRAINT ck_solicitud_peluqueria_estado
        CHECK (estado IN ('SOLICITADA', 'CONFIRMADA', 'RECHAZADA', 'COMPLETADA', 'CANCELADA'));
