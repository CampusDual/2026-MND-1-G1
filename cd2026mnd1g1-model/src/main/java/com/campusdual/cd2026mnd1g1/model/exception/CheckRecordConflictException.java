package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Representa un conflicto al registrar la presencia de una reserva.
 */
public class CheckRecordConflictException extends RuntimeException {

    private final String code;

    public CheckRecordConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
