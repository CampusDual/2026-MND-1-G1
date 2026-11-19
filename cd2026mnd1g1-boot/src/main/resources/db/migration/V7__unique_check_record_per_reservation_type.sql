ALTER TABLE check_registro
    ADD CONSTRAINT uq_check_registro_reserva_tipo UNIQUE (reserva_id, tipo);
