package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos básicos del perro que se muestran en el detalle de una reserva.
 */
public record ReceptionDogDto(
        Long id,
        String name,
        String breed,
        String size,
        LocalDate birthDate,
        String temperament,
        String feedingInstructions,
        boolean informationDeclaredByTutor,
        List<String> allergies,
        List<MedicationDto> medications
) {
}
