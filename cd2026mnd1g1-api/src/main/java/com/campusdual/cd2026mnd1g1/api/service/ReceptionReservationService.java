package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationPageDto;

import java.time.LocalDate;

/**
 * Contrato de consulta de reservas para la recepción.
 */
public interface ReceptionReservationService {

    /**
     * Busca las reservas de una fecha dentro del centro del usuario autenticado.
     *
     * @param date fecha solicitada o nula para usar hoy en la zona del centro
     * @param page índice de página, empezando en cero
     * @param size número máximo de reservas por página
     * @param username usuario autenticado
     * @return página ordenada de reservas
     */
    ReceptionReservationPageDto findReservations(LocalDate date, int page, int size, String username);

    /**
     * Busca el detalle de una reserva dentro del centro del usuario autenticado.
     *
     * @param reservationId identificador de la reserva
     * @param username usuario autenticado
     * @return detalle de la reserva
     */
    ReceptionReservationDetailDto findReservation(Long reservationId, String username);
}
