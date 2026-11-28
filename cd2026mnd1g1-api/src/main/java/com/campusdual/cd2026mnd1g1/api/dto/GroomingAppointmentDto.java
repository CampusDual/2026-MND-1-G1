package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de una cita de peluquería para el listado de recepción.
 */
public record GroomingAppointmentDto(
        Long id,
        String dogName,
        String customerName,
        List<String> serviceNames,
        LocalDate date,
        LocalTime time,
        Integer durationMinutes,
        String location,
        String groomerName,
        GroomingAppointmentStatusDto status,
        GroomingRequestOriginDto origin
) {
}
