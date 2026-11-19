package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Campos permitidos para la ficha de consulta del tutor.
 */
public record TutorDogDetailDto(
        Long id,
        String name,
        String breed,
        String size,
        LocalDate birthDate,
        boolean photoAvailable,
        String feedingInstructions,
        boolean feedingDeclaredByTutor,
        List<TutorDogAllergyDto> allergies,
        List<TutorMedicationDto> medications,
        List<TutorAuthorizedPersonDto> authorizedPersons
) {
    public TutorDogDetailDto {
        allergies = List.copyOf(allergies);
        medications = List.copyOf(medications);
        authorizedPersons = List.copyOf(authorizedPersons);
    }
}
