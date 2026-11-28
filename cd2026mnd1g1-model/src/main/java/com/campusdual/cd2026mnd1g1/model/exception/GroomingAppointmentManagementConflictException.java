package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Conflicto al gestionar una cita de peluquería en un estado no modificable.
 */
public class GroomingAppointmentManagementConflictException extends RuntimeException {

    public GroomingAppointmentManagementConflictException(String message) {
        super(message);
    }
}
