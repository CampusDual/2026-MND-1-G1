package com.campusdual.cd2026mnd1g1.api.dto;

/**
 * Información interna de incompatibilidad visible solo al personal operativo.
 */
public record CaregiverDogIncompatibilityDto(String incompatibleDogName, String reason) {
}
