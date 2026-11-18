package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionReservationRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionAvailabilityDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionBookingCreatedDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionGroupOptionDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Operaciones de reserva y disponibilidad para recepción.
 */
public interface ReceptionBookingService {

    /**
     * Lista los grupos del centro autenticado.
     *
     * @param username usuario autenticado
     * @return grupos ordenados por nombre
     */
    List<ReceptionGroupOptionDto> findGroups(String username);

    /**
     * Consulta de forma informativa las plazas restantes.
     *
     * @param date fecha de reserva
     * @param groupId grupo seleccionado
     * @param username usuario autenticado
     * @return ocupación y plazas restantes por franja
     */
    ReceptionAvailabilityDto findAvailability(LocalDate date, Long groupId, String username);

    /**
     * Comprueba las reglas y crea la reserva confirmada en una transacción.
     *
     * @param request datos de la reserva
     * @param username usuario autenticado
     * @return reserva persistida
     */
    ReceptionBookingCreatedDto createReservation(
            CreateReceptionReservationRequestDto request,
            String username
    );
}
