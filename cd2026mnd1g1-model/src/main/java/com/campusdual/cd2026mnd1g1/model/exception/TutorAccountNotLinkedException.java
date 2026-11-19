package com.campusdual.cd2026mnd1g1.model.exception;

/**
 * Indica que una cuenta con rol tutor todavía no está vinculada a una ficha.
 */
public class TutorAccountNotLinkedException extends RuntimeException {

    public TutorAccountNotLinkedException() {
        super("La cuenta de tutor todavía no está vinculada a una ficha. Contacta con recepción.");
    }
}
