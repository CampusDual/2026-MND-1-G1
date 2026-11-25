package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;

import java.util.List;

/**
 * Consultas de solo lectura para el cuidador autenticado.
 */
public interface CaregiverPortalService {

    /**
     * Lista los perros activos asignados al cuidador para hoy.
     */
    CaregiverDogPageDto findTodayDogs(String username, int page, int size);

    /**
     * Recupera una ficha si su reserva activa pertenece al cuidador.
     */
    CaregiverDogDetailDto findTodayDog(Long reservationId, String username);

    /**
     * Recupera la referencia privada de foto si la reserva pertenece al cuidador.
     */
    String findTodayDogPhotoReference(Long reservationId, String username);

    /**
     * Consulta los registros diarios de una estancia asignada al cuidador.
     */
    List<DailyRecordDto> findTodayDailyRecords(Long reservationId, String username);

    /**
     * Crea un registro diario interno en una estancia asignada al cuidador.
     */
    DailyRecordDto createTodayDailyRecord(
            Long reservationId,
            DailyRecordRequestDto request,
            String username
    );

    /**
     * Consulta el resumen publicado de una estancia asignada.
     */
    CaregiverDailySummaryDto findTodaySummary(Long reservationId, String username);

    /**
     * Publica una vez el resumen diario de una estancia asignada.
     */
    CaregiverDailySummaryDto publishTodaySummary(
            Long reservationId,
            CaregiverDailySummaryRequestDto request,
            String username
    );

    /**
     * Consulta las incidencias internas de una estancia asignada.
     */
    List<IncidentDto> findTodayIncidents(Long reservationId, String username);

    /**
     * Registra una incidencia interna en una estancia asignada.
     */
    IncidentDto createTodayIncident(
            Long reservationId,
            IncidentRequestDto request,
            String username
    );
}
