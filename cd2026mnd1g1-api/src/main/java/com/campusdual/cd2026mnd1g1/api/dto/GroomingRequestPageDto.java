package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página ordenada de solicitudes de peluquería.
 */
public record GroomingRequestPageDto(
        List<GroomingRequestDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
