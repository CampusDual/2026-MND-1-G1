package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.TutorDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogCreateResultDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.TutorAuthorizedPersonDto;

import java.util.List;

/**
 * Consultas privadas del tutor autenticado sobre sus perros.
 */
public interface TutorPortalService {

    /**
     * Lista perros asociados al tutor vinculado a la cuenta autenticada.
     *
     * @param username usuario autenticado
     * @param page índice de página
     * @param size número de resultados por página
     * @return página de perros visibles
     */
    TutorDogPageDto findMyDogs(String username, int page, int size);

    /**
     * Recupera una ficha solo si el perro pertenece al tutor autenticado.
     *
     * @param dogId identificador del perro
     * @param username usuario autenticado
     * @return ficha con campos expresamente permitidos
     */
    TutorDogDetailDto findMyDog(Long dogId, String username);

    /**
     * Resuelve la referencia interna de foto después de comprobar la propiedad.
     *
     * @param dogId identificador del perro
     * @param username usuario autenticado
     * @return referencia almacenada, no destinada a la respuesta JSON
     */
    String findMyDogPhotoReference(Long dogId, String username);

    /**
     * Consulta las autorizaciones generales del tutor autenticado.
     *
     * @param username usuario autenticado
     * @return autorizaciones de nombre y teléfono
     */
    List<TutorAuthorizedPersonDto> findMyAuthorizedPersons(String username);

    /**
     * Registra un perro y sus datos declarados por el tutor autenticado.
     *
     * @param request datos permitidos del perro
     * @param username usuario autenticado
     * @param idempotencyKey clave de reintento
     * @param payloadHash huella del contenido enviado
     * @param photoReference nombre local y aleatorio de la foto, si existe
     * @return ficha creada o resultado previo idempotente
     */
    TutorDogCreateResultDto createMyDog(
            TutorDogCreateRequestDto request,
            String username,
            String idempotencyKey,
            String payloadHash,
            String photoReference
    );
}
