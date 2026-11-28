package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Nuevo hueco solicitado para una cita de peluquería.
 */
public record RescheduleGroomingAppointmentRequestDto(
        @NotNull LocalDate date,
        @NotNull LocalTime time
) {
}
