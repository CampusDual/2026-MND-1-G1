package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Datos editables permitidos para registrar la llegada en recepción.
 */
public record CreateReceptionArrivalRequestDto(
        @Size(max = 2000, message = "Las observaciones de llegada no pueden superar 2000 caracteres")
        String observations
) {
}
