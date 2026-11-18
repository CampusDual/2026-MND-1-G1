package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Grupo disponible para reservar dentro del centro autorizado.
 */
public record ReceptionGroupOptionDto(
        Long id,
        String name,
        int capacityPerSlot
) {
}
