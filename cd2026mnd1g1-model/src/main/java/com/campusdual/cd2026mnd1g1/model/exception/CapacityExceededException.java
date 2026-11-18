package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que no queda aforo en una o más franjas del grupo.
 */
public class CapacityExceededException extends RuntimeException {

    public CapacityExceededException(String message) {
        super(message);
    }
}
