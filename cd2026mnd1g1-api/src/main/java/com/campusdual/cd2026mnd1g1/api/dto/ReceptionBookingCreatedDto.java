package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Confirmación de una reserva creada desde recepción.
 */
public record ReceptionBookingCreatedDto(
        Long id,
        LocalDate date,
        String modality,
        String status,
        Long dogId,
        String dogName,
        Long groupId,
        String groupName
) {
}
