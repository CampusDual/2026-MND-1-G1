package com.campusdual.cd2026mnd1g1.api.dto;

import java.util.List;

/**
 * Ficha de un perro con las personas autorizadas por su tutor.
 */
public record DogDetailDto(
        DogDto dog,
        List<AuthorizedPersonDto> authorizedPersons
) {
}
