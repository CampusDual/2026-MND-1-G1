package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Citas asignadas a un peluquero para un día concreto.
 */
public record GroomerAgendaDto(
        Long groomerId,
        String groomerName,
        LocalDate date,
        List<GroomingAppointmentDto> appointments
) {
}
