package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Informa de una clave de alta reutilizada con datos distintos.
 */
public class DuplicateTutorDogSubmissionException extends RuntimeException {

    public DuplicateTutorDogSubmissionException(String message) {
        super(message);
    }
}
