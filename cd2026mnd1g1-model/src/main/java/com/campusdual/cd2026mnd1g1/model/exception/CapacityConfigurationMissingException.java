package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que el centro todavía no tiene grupos con capacidad configurada.
 */
public class CapacityConfigurationMissingException extends RuntimeException {

    public CapacityConfigurationMissingException(String message) {
        super(message);
    }
}
