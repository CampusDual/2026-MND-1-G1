package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Información necesaria para consultar el detalle de una reserva en recepción.
 */
public record ReceptionReservationDetailDto(
        Long id,
        LocalDate date,
        String modality,
        String status,
        ReceptionDogDto dog,
        ReceptionTutorDto tutor,
        ReceptionPresenceDto presence
) {
}
