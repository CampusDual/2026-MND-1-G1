ALTER TABLE perro
    ADD COLUMN alimentacion_declarada_por_tutor BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE perro
    ADD COLUMN temperamento_declarado_por_tutor BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE perro
    ADD COLUMN actualizado_por_usuario_id BIGINT;

ALTER TABLE perro
    ADD COLUMN actualizado_en TIMESTAMP;

ALTER TABLE perro
    ADD CONSTRAINT fk_perro_actualizado_por_usuario
    FOREIGN KEY (actualizado_por_usuario_id) REFERENCES USUARIOS (id);

ALTER TABLE alergia
    ADD COLUMN declarada_por_tutor BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE medicacion
    ADD COLUMN declarada_por_tutor BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE persona_autorizada
    ADD COLUMN actualizado_por_usuario_id BIGINT;

ALTER TABLE persona_autorizada
    ADD COLUMN actualizado_en TIMESTAMP;

ALTER TABLE persona_autorizada
    ADD COLUMN revocado_por_usuario_id BIGINT;

ALTER TABLE persona_autorizada
    ADD COLUMN revocado_en TIMESTAMP;

ALTER TABLE persona_autorizada
    ADD CONSTRAINT fk_persona_autorizada_actualizado_por
    FOREIGN KEY (actualizado_por_usuario_id) REFERENCES USUARIOS (id);

ALTER TABLE persona_autorizada
    ADD CONSTRAINT fk_persona_autorizada_revocado_por
    FOREIGN KEY (revocado_por_usuario_id) REFERENCES USUARIOS (id);
