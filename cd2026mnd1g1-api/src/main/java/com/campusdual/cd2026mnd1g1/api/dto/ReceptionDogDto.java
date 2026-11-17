package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDate;

/**
 * Datos básicos del perro que se muestran en el detalle de una reserva.
 */
public record ReceptionDogDto(
        Long id,
        String name,
        String breed,
        String size,
        LocalDate birthDate
) {
}
