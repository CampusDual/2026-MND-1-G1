package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Datos básicos de un perro visibles para recepción.
 */
public record DogDto(
        Long id,
        String name,
        String breed,
        String size,
        LocalDate birthDate,
        String feedingInstructions,
        String temperament,
        Long tutorId,
        String tutorName,
        String tutorNationalId
) {
}
