package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Campos editables por el tutor en una ficha propia.
 */
public record TutorDogUpdateRequestDto(
        @NotBlank(message = "El nombre del perro es obligatorio")
        @Size(max = 120, message = "El nombre del perro no puede superar 120 caracteres")
        String name,

        @Size(max = 120, message = "La raza no puede superar 120 caracteres")
        String breed,

        @NotBlank(message = "El tamaño es obligatorio")
        @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "El tamaño no es válido")
        String size,

        LocalDate birthDate,

        @NotNull(message = "La lista de alergias es obligatoria; puede estar vacía")
        @Valid
        List<@Valid TutorDogAllergyInputDto> allergies,

        @NotNull(message = "La lista de medicación es obligatoria; puede estar vacía")
        @Valid
        List<@Valid TutorDogMedicationInputDto> medications,

        @NotNull(message = "La lista de personas autorizadas es obligatoria; puede estar vacía")
        @Valid
        List<@Valid TutorDogAuthorizedPersonEditInputDto> authorizedPersons,

        boolean removePhoto
) {
}
