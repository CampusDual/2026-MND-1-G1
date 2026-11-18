package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos admitidos al registrar un perro para la guardería interna.
 */
public record DogCreateRequestDto(
        @NotNull(message = "Debes seleccionar un tutor")
        @Min(value = 0, message = "El identificador del tutor no puede ser negativo")
        Long tutorId,

        @NotBlank(message = "El nombre del perro es obligatorio")
        @Size(max = 120, message = "El nombre del perro no puede superar 120 caracteres")
        String name,

        @Size(max = 120, message = "La raza no puede superar 120 caracteres")
        String breed,

        @NotBlank(message = "El tamaño es obligatorio")
        @Pattern(regexp = "PEQUENO|MEDIANO|GRANDE", message = "El tamaño no es válido")
        String size,

        LocalDate birthDate,

        boolean confirmMatches
) {
}
