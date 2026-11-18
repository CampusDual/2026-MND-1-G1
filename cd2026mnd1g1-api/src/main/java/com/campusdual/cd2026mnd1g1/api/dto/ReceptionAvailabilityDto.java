package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Disponibilidad informativa de un grupo para una fecha.
 */
public record ReceptionAvailabilityDto(
        LocalDate date,
        Long groupId,
        String groupName,
        int capacityPerSlot,
        long morningOccupied,
        long morningRemaining,
        long afternoonOccupied,
        long afternoonRemaining
) {
}
