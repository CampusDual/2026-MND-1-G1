package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contenido autorizado que el cuidador publica para el tutor.
 */
public record CaregiverDailySummaryRequestDto(
        @NotBlank(message = "El resumen diario es obligatorio")
        @Size(max = 4000, message = "El resumen no puede superar 4000 caracteres")
        String text
) {
}
