package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.TutorCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorUpdateRequestDto;

/**
 * Operaciones de consulta y mantenimiento de tutores.
 */
public interface TutorService {

    /**
     * Busca tutores del centro del usuario autenticado.
     *
     * @param search texto de búsqueda
     * @param page índice de página
     * @param size tamaño de página
     * @param username usuario autenticado
     * @return página de tutores
     */
    TutorPageDto findTutors(String search, int page, int size, String username);

    /**
     * Recupera una ficha de tutor del centro autorizado.
     *
     * @param id identificador del tutor
     * @param username usuario autenticado
     * @return ficha del tutor
     */
    TutorDto findTutor(Long id, String username);

    /**
     * Registra un tutor en el centro autorizado.
     *
     * @param request datos del tutor
     * @param username usuario autenticado
     * @return tutor creado
     */
    TutorDto createTutor(TutorCreateRequestDto request, String username);

    /**
     * Actualiza los datos operativos de un tutor.
     *
     * @param id identificador del tutor
     * @param request datos modificados
     * @param username usuario autenticado
     * @return tutor actualizado
     */
    TutorDto updateTutor(Long id, TutorUpdateRequestDto request, String ifMatch, String username);
}
