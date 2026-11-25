package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestStatusDto;

import java.time.LocalDate;

/**
 * Contrato de consulta de solicitudes de peluquería para recepción.
 */
public interface ReceptionGroomingRequestService {

    /**
     * Busca solicitudes de peluquería limitadas al centro del usuario autenticado.
     *
     * @param origin filtro opcional por origen
     * @param status filtro opcional por estado
     * @param fromDate inicio opcional del rango inclusivo
     * @param toDate fin opcional del rango inclusivo
     * @param page índice de página empezando en cero
     * @param size número de resultados por página
     * @param username usuario autenticado
     * @return página ordenada de solicitudes
     */
    GroomingRequestPageDto findRequests(
            GroomingRequestOriginDto origin,
            GroomingRequestStatusDto status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String username
    );

    /**
     * Consulta una solicitud únicamente si pertenece al ámbito de peluquería del usuario.
     *
     * @param requestId identificador de solicitud
     * @param username usuario autenticado
     * @return detalle de la solicitud
     */
    GroomingRequestDetailDto findRequest(Long requestId, String username);

    /**
     * Confirma una solicitud pendiente del centro del usuario autenticado.
     *
     * @param requestId identificador de solicitud
     * @param username usuario autenticado
     * @return detalle actualizado de la solicitud
     */
    GroomingRequestDetailDto confirmRequest(Long requestId, String username);

    /**
     * Rechaza una solicitud pendiente del centro del usuario autenticado.
     *
     * @param requestId identificador de solicitud
     * @param username usuario autenticado
     * @return detalle actualizado de la solicitud
     */
    GroomingRequestDetailDto rejectRequest(Long requestId, String username);
}
