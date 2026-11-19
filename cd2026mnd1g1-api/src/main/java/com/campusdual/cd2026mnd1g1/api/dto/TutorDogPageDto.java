package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Página de perros asociados a la cuenta de tutor autenticada.
 */
public record TutorDogPageDto(
        List<TutorDogSummaryDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public TutorDogPageDto {
        items = List.copyOf(items);
    }
}
