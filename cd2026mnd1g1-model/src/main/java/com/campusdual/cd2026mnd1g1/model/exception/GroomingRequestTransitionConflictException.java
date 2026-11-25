package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que una solicitud ya no está pendiente y no admite esta decisión.
 */
public class GroomingRequestTransitionConflictException extends RuntimeException {

    public GroomingRequestTransitionConflictException(String message) {
        super(message);
    }
}
