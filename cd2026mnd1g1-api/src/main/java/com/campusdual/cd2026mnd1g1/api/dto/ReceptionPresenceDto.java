package com.campusdual.cd2026mnd1g1.api.dto;

import java.time.LocalDateTime;

/**
 * Presencia calculada exclusivamente a partir de los registros de entrada y salida.
 */
public record ReceptionPresenceDto(
        String status,
        LocalDateTime lastEntryAt,
        String entryPetCondition,
        LocalDateTime lastExitAt,
        boolean inconsistent
) {
}
