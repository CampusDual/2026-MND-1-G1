package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Datos visibles de un tutor dentro del centro autorizado.
 */
public record TutorDto(
        Long id,
        String firstName,
        String lastName,
        String nationalId,
        String phone,
        String email
) {
}
