package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ficha diaria orientada al cuidado del perro.
 */
public record CaregiverDogDetailDto(
        Long reservationId,
        Long id,
        String name,
        String breed,
        String size,
        LocalDate birthDate,
        CaregiverTutorContactDto tutor,
        boolean photoAvailable,
        LocalDateTime arrivalAt,
        String stayStatus,
        String groupName,
        String feedingInstructions,
        String temperament,
        List<CaregiverDogAllergyDto> allergies,
        List<CaregiverDogMedicationDto> medications,
        List<CaregiverDogIncompatibilityDto> incompatibilities
) {
}
