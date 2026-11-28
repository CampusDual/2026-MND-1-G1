package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Perro interno disponible para seleccionar al crear una cita.
 */
public record GroomingDogOptionDto(
        Long id,
        String name,
        String tutorName
) {
}
