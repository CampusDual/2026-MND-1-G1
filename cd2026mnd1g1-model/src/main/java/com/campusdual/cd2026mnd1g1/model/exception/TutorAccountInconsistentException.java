package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que la relación entre cuenta, tutor y centro no es coherente.
 */
public class TutorAccountInconsistentException extends RuntimeException {

    public TutorAccountInconsistentException() {
        super("No se puede consultar el portal por una incidencia de configuración. Contacta con soporte.");
    }
}
