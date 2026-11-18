package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página de perros del centro autorizado.
 */
public record DogPageDto(
        List<DogDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
