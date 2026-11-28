package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página de perros internos para el selector de citas.
 */
public record GroomingDogOptionPageDto(
        List<GroomingDogOptionDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
