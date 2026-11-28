package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Resumen diario publicado visible para un tutor.
 */
public record TutorDailySummaryDto(
        Long id,
        LocalDate date,
        String dogName,
        String status,
        boolean viewed
) {
}
