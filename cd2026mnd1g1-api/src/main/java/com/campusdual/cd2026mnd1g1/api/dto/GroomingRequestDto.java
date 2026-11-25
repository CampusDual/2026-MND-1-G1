package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de una solicitud de peluquería para el listado de recepción.
 */
public record GroomingRequestDto(
        Long id,
        String dogName,
        String customerName,
        List<String> serviceNames,
        LocalDate requestedDate,
        LocalTime requestedTime,
        GroomingRequestStatusDto status,
        GroomingRequestOriginDto origin
) {
}
