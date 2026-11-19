package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Datos mínimos de un perro visibles en la lista privada del tutor.
 */
public record TutorDogSummaryDto(
        Long id,
        String name,
        String breed,
        String size,
        boolean photoAvailable
) {
}
