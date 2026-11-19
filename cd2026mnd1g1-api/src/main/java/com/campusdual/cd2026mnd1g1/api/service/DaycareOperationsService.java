package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionArrivalRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.RegisterReceptionDepartureRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReservationDayDto;

import java.time.LocalDate;
import java.util.List;

public interface DaycareOperationsService {

    /**
     * Recupera las reservas de una fecha para el centro del usuario autenticado.
     *
     * @param date fecha de las reservas; si es nula se usa el día actual
     * @param groupId filtro opcional por grupo
     * @param username nombre de usuario autenticado, que debe ser su email operativo
     * @return reservas con ficha del perro y ocupación del grupo
     */
    List<ReservationDayDto> findReservations(LocalDate date, Long groupId, String username);

    /**
     * Registra una llegada por el contrato legado; las salidas requieren el flujo de entrega de recepción.
     *
     * @param reservationId identificador de la reserva
     * @param request datos del registro de entrada
     * @param username nombre de usuario autenticado
     * @return registro creado
     */
    CheckRecordDto createCheckRecord(Long reservationId, CheckRecordRequestDto request, String username);

    /**
     * Registra la llegada de una reserva elegible con la hora y usuario de la sesión.
     *
     * @param reservationId identificador de la reserva
     * @param request observaciones opcionales de llegada
     * @param username usuario autenticado
     * @return registro de entrada persistido
     */
    CheckRecordDto registerArrival(
            Long reservationId,
            CreateReceptionArrivalRequestDto request,
            String username
    );

    /**
     * Registra la salida de una estancia abierta y la persona que recibe al perro.
     *
     * @param reservationId identificador de la reserva
     * @param request datos del destinatario y declaración de pertenencias
     * @param username usuario autenticado
     * @return registro de salida persistido
     */
    CheckRecordDto registerDeparture(
            Long reservationId,
            RegisterReceptionDepartureRequestDto request,
            String username
    );

    /**
     * Registra una actividad diaria para una reserva.
     *
     * @param reservationId identificador de la reserva
     * @param request datos de la actividad
     * @param username nombre de usuario autenticado
     * @return registro diario creado
     */
    DailyRecordDto createDailyRecord(Long reservationId, DailyRecordRequestDto request, String username);

    /**
     * Registra una incidencia asociada a una reserva.
     *
     * @param reservationId identificador de la reserva
     * @param request datos de la incidencia
     * @param username nombre de usuario autenticado
     * @return incidencia creada
     */
    IncidentDto createIncident(Long reservationId, IncidentRequestDto request, String username);
}
