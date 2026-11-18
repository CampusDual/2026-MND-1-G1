package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.DogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogUpdateRequestDto;

/**
 * Operaciones de gestión interna de perros para recepción.
 */
public interface DogService {

    /**
     * Busca perros del centro del usuario autenticado.
     *
     * @param search texto de búsqueda opcional
     * @param page índice de página
     * @param size número de resultados por página
     * @param username usuario autenticado
     * @return resultados paginados
     */
    DogPageDto findDogs(String search, int page, int size, String username);

    /**
     * Recupera una ficha dentro del centro del usuario autenticado.
     *
     * @param id identificador del perro
     * @param username usuario autenticado
     * @return ficha del perro
     */
    DogDto findDog(Long id, String username);

    /**
     * Registra un perro asociado a un tutor del centro autorizado.
     *
     * @param request datos del perro
     * @param username usuario autenticado
     * @return ficha creada
     */
    DogDto createDog(DogCreateRequestDto request, String username);

    /**
     * Actualiza solo los datos básicos permitidos de un perro.
     *
     * @param id identificador del perro
     * @param request campos editables
     * @param ifMatch versión leída antes de editar
     * @param username usuario autenticado
     * @return ficha actualizada
     */
    DogDto updateDog(Long id, DogUpdateRequestDto request, String ifMatch, String username);
}
