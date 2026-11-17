package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página de reservas con información para continuar la consulta.
 */
public record ReceptionReservationPageDto(
        List<ReceptionReservationDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
