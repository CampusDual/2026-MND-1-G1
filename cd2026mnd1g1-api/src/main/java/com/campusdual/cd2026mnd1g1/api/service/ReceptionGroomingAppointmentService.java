package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentStatusDto;

import java.time.LocalDate;

/**
 * Contrato de consulta de citas de peluquería para recepción.
 */
public interface ReceptionGroomingAppointmentService {

    /**
     * Busca citas limitadas al centro del usuario autenticado.
     *
     * @param origin filtro opcional por origen
     * @param status filtro opcional por estado
     * @param fromDate inicio opcional del rango inclusivo
     * @param toDate fin opcional del rango inclusivo
     * @param page índice de página empezando en cero
     * @param size número de resultados por página
     * @param username usuario autenticado
     * @return página ordenada de citas
     */
    GroomingAppointmentPageDto findAppointments(
            GroomingRequestOriginDto origin,
            GroomingAppointmentStatusDto status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String username
    );

    /**
     * Consulta una cita únicamente si pertenece al centro del usuario.
     *
     * @param appointmentId identificador de cita
     * @param username usuario autenticado
     * @return detalle de la cita
     */
    GroomingAppointmentDetailDto findAppointment(Long appointmentId, String username);
}
