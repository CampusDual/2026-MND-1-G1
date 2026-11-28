package com.campusdual.cd2026mnd1g1.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo aportado al cancelar una cita de peluquería.
 */
public record CancelGroomingAppointmentRequestDto(
        @NotBlank @Size(max = 2000) String reason
) {
}
