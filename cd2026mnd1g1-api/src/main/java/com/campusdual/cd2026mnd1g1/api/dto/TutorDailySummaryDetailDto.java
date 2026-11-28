package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contenido publicado de un resumen diario accesible para su tutor.
 */
public record TutorDailySummaryDetailDto(
        Long id,
        Long dogId,
        LocalDate date,
        String dogName,
        String status,
        boolean viewed,
        String text,
        LocalDateTime publishedAt
) {
}
