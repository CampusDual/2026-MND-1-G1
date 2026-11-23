package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página de perros asignados al cuidador autenticado.
 */
public record CaregiverDogPageDto(
        List<CaregiverDogSummaryDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
