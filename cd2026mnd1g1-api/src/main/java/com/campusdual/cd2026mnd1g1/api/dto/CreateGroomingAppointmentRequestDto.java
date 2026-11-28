package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos permitidos para crear una cita interna de peluquería.
 */
public record CreateGroomingAppointmentRequestDto(
        @NotNull @Min(0) Long dogId,
        @NotNull @Min(0) Long serviceId,
        @NotNull LocalDate date,
        @NotNull LocalTime time,
        @Size(max = 2000) String notes
) {
}
