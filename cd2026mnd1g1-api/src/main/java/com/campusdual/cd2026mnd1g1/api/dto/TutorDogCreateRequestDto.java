package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Campos permitidos para registrar un perro desde el portal tutor.
 */
public record TutorDogCreateRequestDto(
        @NotBlank(message = "El nombre del perro es obligatorio")
        @Size(max = 120, message = "El nombre del perro no puede superar 120 caracteres")
        String name,

        @Size(max = 120, message = "La raza no puede superar 120 caracteres")
        String breed,

        @NotBlank(message = "El tamaño es obligatorio")
        @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "El tamaño no es válido")
        String size,

        LocalDate birthDate,

        @Size(max = 1000, message = "Las pautas de alimentación no pueden superar 1000 caracteres")
        String feedingInstructions,

        @Size(max = 1000, message = "El temperamento no puede superar 1000 caracteres")
        String temperament,

        @Valid
        List<@Valid TutorDogAllergyInputDto> allergies,

        @Valid
        List<@Valid TutorDogMedicationInputDto> medications,

        @Valid
        List<@Valid TutorDogAuthorizedPersonInputDto> authorizedPersons
) {
}
