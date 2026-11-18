package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que el perro ya tiene una reserva activa para la fecha solicitada.
 */
public class DuplicateReservationException extends RuntimeException {

    public DuplicateReservationException(String message) {
        super(message);
    }
}
