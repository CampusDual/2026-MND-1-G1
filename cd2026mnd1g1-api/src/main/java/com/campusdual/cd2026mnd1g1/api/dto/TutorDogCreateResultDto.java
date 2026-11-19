package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Resultado de creación; indica si se devolvió una operación idempotente previa.
 */
public record TutorDogCreateResultDto(TutorDogDetailDto dog, boolean replayed) {
}
