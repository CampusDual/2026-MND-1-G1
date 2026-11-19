package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Pauta de medicación registrada; no representa una dosis administrada.
 */
public record TutorMedicationDto(
        String medication,
        String dosage,
        String instructions,
        boolean declaredByTutor
) {
}
