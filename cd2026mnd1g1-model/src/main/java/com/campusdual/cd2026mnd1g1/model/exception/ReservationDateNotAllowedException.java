package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que la fecha no está permitida por las reglas operativas del centro.
 */
public class ReservationDateNotAllowedException extends RuntimeException {

    public ReservationDateNotAllowedException(String message) {
        super(message);
    }
}
