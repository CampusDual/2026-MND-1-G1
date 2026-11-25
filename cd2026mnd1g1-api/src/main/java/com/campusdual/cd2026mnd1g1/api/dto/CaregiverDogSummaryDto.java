package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

/**
 * Datos mínimos de un perro asignado al cuidador para hoy.
 */
public record CaregiverDogSummaryDto(
        Long reservationId,
        Long id,
        String name,
        String breed,
        String size,
        boolean photoAvailable,
        LocalDateTime arrivalAt,
        String stayStatus,
        String groupName
) {
}
