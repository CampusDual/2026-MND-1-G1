package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Campos permitidos al actualizar un tutor.
 */
public record TutorUpdateRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String firstName,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 160, message = "Los apellidos no pueden superar 160 caracteres")
        String lastName,

        @NotBlank(message = "El DNI es obligatorio")
        @Size(max = 20, message = "El DNI no puede superar 20 caracteres")
        String nationalId,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 254, message = "El email no puede superar 254 caracteres")
        String email,

        boolean confirmMatches
) {
}
