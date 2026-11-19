package com.campusdual.cd2026mnd1g1.ws.controller;

/**
 * Error de configuración o persistencia del almacenamiento privado de fotos.
 */
public class TutorDogPhotoStorageException extends RuntimeException {

    public TutorDogPhotoStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public TutorDogPhotoStorageException(String message) {
        super(message);
    }
}
