package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordRequestDto;
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
     * Registra una entrada o salida y actualiza el estado de la reserva.
     *
     * @param reservationId identificador de la reserva
     * @param request datos del registro de entrada o salida
     * @param username nombre de usuario autenticado
     * @return registro creado
     */
    CheckRecordDto createCheckRecord(Long reservationId, CheckRecordRequestDto request, String username);

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
