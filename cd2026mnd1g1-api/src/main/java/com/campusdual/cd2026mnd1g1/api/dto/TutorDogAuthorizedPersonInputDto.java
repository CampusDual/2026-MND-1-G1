package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Persona autorizada por el tutor para recoger sus perros.
 */
public record TutorDogAuthorizedPersonInputDto(
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String name,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone
) {
}
