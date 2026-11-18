package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * Datos permitidos para crear una reserva desde recepción.
 */
public record CreateReceptionReservationRequestDto(
        @NotNull @PositiveOrZero Long dogId,
        @NotNull @PositiveOrZero Long groupId,
        @NotNull LocalDate date,
        @NotNull @Pattern(regexp = "DIA_COMPLETO|MANANA|TARDE") String modality
) {
}
