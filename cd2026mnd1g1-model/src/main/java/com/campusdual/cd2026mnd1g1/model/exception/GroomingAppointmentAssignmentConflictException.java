package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que una cita ya no está pendiente de asignación.
 */
public class GroomingAppointmentAssignmentConflictException extends RuntimeException {

    public GroomingAppointmentAssignmentConflictException(String message) {
        super(message);
    }
}
