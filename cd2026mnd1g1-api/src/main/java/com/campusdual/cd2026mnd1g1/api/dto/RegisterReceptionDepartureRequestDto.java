package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar la persona que recoge y las observaciones opcionales de salida.
 */
public record RegisterReceptionDepartureRequestDto(
        @NotNull(message = "Debe indicarse quién recoge al perro")
        CollectorTypeDto collectorType,
        @PositiveOrZero(message = "El identificador de persona autorizada no puede ser negativo")
        Long authorizedPersonId,
        @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
        String observations
) {
}
