ALTER TABLE check_registro
    ADD COLUMN recogido_por_tutor_id BIGINT;

ALTER TABLE check_registro
    ADD COLUMN recogido_por_persona_autorizada_id BIGINT;

ALTER TABLE check_registro
    ADD COLUMN recogido_nombre VARCHAR(120);

ALTER TABLE check_registro
    ADD CONSTRAINT fk_check_registro_recogido_tutor
        FOREIGN KEY (recogido_por_tutor_id) REFERENCES tutor (id);

ALTER TABLE check_registro
    ADD CONSTRAINT fk_check_registro_recogido_autorizado
        FOREIGN KEY (recogido_por_persona_autorizada_id) REFERENCES persona_autorizada (id);

ALTER TABLE check_registro
    ADD CONSTRAINT ck_check_registro_recogida_alternativa CHECK (
        (
            recogido_por_tutor_id IS NULL
            AND recogido_por_persona_autorizada_id IS NULL
            AND recogido_nombre IS NULL
        )
        OR (
            recogido_por_tutor_id IS NOT NULL
            AND recogido_por_persona_autorizada_id IS NULL
            AND recogido_nombre IS NOT NULL
        )
        OR (
            recogido_por_tutor_id IS NULL
            AND recogido_por_persona_autorizada_id IS NOT NULL
            AND recogido_nombre IS NOT NULL
        )
    );
