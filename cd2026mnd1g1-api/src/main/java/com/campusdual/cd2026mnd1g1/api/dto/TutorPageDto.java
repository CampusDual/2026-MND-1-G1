package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página paginada de tutores.
 */
public record TutorPageDto(
        List<TutorDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
