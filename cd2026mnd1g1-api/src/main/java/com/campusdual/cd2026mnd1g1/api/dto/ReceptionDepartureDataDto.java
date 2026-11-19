package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Datos de solo lectura y personas registradas para preparar la salida.
 */
public record ReceptionDepartureDataDto(
        Long reservationId,
        LocalDate date,
        String modality,
        String reservationStatus,
        String dogName,
        Long tutorId,
        String tutorName,
        LocalDateTime entryAt,
        String entryObservations,
        String presenceStatus,
        List<AuthorizedPersonDto> authorizedPersons
) {
}
