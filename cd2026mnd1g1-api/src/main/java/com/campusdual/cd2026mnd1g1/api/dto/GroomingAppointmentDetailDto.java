package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de consulta de una cita de peluquería.
 */
public record GroomingAppointmentDetailDto(
        Long id,
        String dogName,
        String customerName,
        List<String> serviceNames,
        LocalDate date,
        LocalTime time,
        Integer durationMinutes,
        String location,
        Long groomerId,
        String groomerName,
        GroomingAppointmentStatusDto status,
        GroomingRequestOriginDto origin,
        String notes,
        String cancellationReason
) {
}
