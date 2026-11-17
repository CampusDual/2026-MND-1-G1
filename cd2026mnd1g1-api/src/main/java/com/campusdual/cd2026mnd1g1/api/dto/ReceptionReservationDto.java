package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Datos mínimos de una reserva para el listado de recepción.
 */
public record ReceptionReservationDto(
        Long id,
        LocalDate date,
        String modality,
        String status,
        Long dogId,
        String dogName,
        String tutorName,
        String presenceStatus
) {
}
