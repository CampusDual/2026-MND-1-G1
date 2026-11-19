package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pauta declarada por el tutor; no implica revisión ni administración.
 */
public record TutorDogMedicationInputDto(
        @NotBlank(message = "El nombre del medicamento es obligatorio")
        @Size(max = 160, message = "El medicamento no puede superar 160 caracteres")
        String medication,

        @Size(max = 255, message = "La dosis no puede superar 255 caracteres")
        String dosage,

        @NotBlank(message = "La pauta del medicamento es obligatoria")
        @Size(max = 1000, message = "La pauta no puede superar 1000 caracteres")
        String instructions
) {
}
