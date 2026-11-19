package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Resultado interno de lectura o actualización con control de versión y limpieza de foto.
 */
public record TutorDogEditResultDto(
        TutorDogDetailDto dog,
        String etag,
        String previousPhotoReference,
        boolean photoChanged
) {
}
