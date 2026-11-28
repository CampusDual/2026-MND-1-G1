package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.CreateGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.AssignGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CancelGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.RescheduleGroomingAppointmentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingDogOptionPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingServiceOptionDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingRequestOriginDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomingAppointmentStatusDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomerAgendaDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroomerOptionDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de consulta de citas de peluquería para recepción.
 */
public interface ReceptionGroomingAppointmentService {

    /**
     * Busca perros de la guardería interna del centro autenticado.
     *
     * @param search filtro opcional por nombre
     * @param page índice de página empezando en cero
     * @param size número de resultados por página
     * @param username usuario autenticado
     * @return página de perros elegibles
     */
    GroomingDogOptionPageDto findInternalDogs(String search, int page, int size, String username);

    /**
     * Lista los servicios activos del centro autenticado.
     *
     * @param username usuario autenticado
     * @return servicios elegibles ordenados por nombre
     */
    List<GroomingServiceOptionDto> findAvailableServices(String username);

    /**
     * Crea una cita interna pendiente de asignación.
     *
     * @param request datos permitidos de la cita
     * @param username usuario autenticado
     * @return detalle persistido de la cita
     */
    GroomingAppointmentDetailDto createAppointment(CreateGroomingAppointmentRequestDto request, String username);

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

    /**
     * Asigna peluquero y lugar a una cita pendiente dentro del centro autenticado.
     *
     * @param appointmentId identificador de cita
     * @param request peluquero, lugar y hueco confirmado
     * @param username usuario autenticado
     * @return cita asignada
     */
    GroomingAppointmentDetailDto assignAppointment(
            Long appointmentId,
            AssignGroomingAppointmentRequestDto request,
            String username
    );

    /**
     * Cambia la fecha y hora sin alterar el estado ni la asignación.
     *
     * @param appointmentId identificador de cita
     * @param request nuevo hueco
     * @param username usuario autenticado
     * @return cita reprogramada
     */
    GroomingAppointmentDetailDto rescheduleAppointment(
            Long appointmentId,
            RescheduleGroomingAppointmentRequestDto request,
            String username
    );

    /**
     * Cancela una cita sin eliminarla y conserva el motivo aportado.
     *
     * @param appointmentId identificador de cita
     * @param request motivo de cancelación
     * @param username usuario autenticado
     * @return cita cancelada
     */
    GroomingAppointmentDetailDto cancelAppointment(
            Long appointmentId,
            CancelGroomingAppointmentRequestDto request,
            String username
    );

    /**
     * Lista peluqueros activos del centro de recepción.
     *
     * @param username usuario autenticado
     * @return peluqueros ordenados por nombre
     */
    List<GroomerOptionDto> findGroomers(String username);

    /**
     * Consulta citas asignadas a un peluquero en un día concreto.
     *
     * @param groomerId identificador del peluquero
     * @param date día consultado
     * @param username usuario autenticado
     * @return agenda de solo lectura
     */
    GroomerAgendaDto findGroomerAgenda(Long groomerId, LocalDate date, String username);

    /**
     * Consulta una cita únicamente si está asignada al peluquero indicado y pertenece al centro.
     *
     * @param groomerId identificador del peluquero
     * @param appointmentId identificador de cita
     * @param username usuario autenticado
     * @return detalle de cita
     */
    GroomingAppointmentDetailDto findGroomerAgendaAppointment(
            Long groomerId,
            Long appointmentId,
            String username
    );
}
