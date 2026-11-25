package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página ordenada de citas de peluquería.
 */
public record GroomingAppointmentPageDto(
        List<GroomingAppointmentDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
