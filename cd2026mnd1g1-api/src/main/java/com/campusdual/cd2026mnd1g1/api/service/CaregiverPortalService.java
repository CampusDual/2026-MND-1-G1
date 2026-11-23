package com.campusdual.cd2026mnd1g1.api.service;

import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;

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
}
