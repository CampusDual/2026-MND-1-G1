package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos permitidos para asignar una cita pendiente de peluquería.
 */
public record AssignGroomingAppointmentRequestDto(
        @NotNull @Min(0) Long groomerId,
        @NotNull GroomingAppointmentLocationDto location,
        @NotNull LocalDate date,
        @NotNull LocalTime time
) {
}
