package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Pauta de medicación registrada; no representa una administración.
 */
public record CaregiverDogMedicationDto(
        Long id,
        String medication,
        String dosage,
        String instructions,
        boolean declaredByTutor
) {
}
