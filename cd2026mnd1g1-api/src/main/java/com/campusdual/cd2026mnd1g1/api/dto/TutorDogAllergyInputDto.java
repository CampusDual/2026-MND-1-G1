package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Declaración de alergia facilitada por el tutor durante el alta.
 */
public record TutorDogAllergyInputDto(
        @NotBlank(message = "La descripción de cada alergia es obligatoria")
        @Size(max = 1000, message = "La descripción de la alergia no puede superar 1000 caracteres")
        String description
) {
}
