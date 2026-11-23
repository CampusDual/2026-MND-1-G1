package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

/**
 * Resumen publicado para consulta del tutor.
 */
public record CaregiverDailySummaryDto(
        Long id,
        Long reservationId,
        String text,
        String status,
        LocalDateTime publishedAt
) {
}
