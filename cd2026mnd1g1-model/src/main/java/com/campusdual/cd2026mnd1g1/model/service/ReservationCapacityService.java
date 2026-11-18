package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationModality;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityExceededException;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Calcula y valida el aforo independiente de cada franja.
 */
@Service
@Transactional(readOnly = true)
public class ReservationCapacityService {

    private static final List<ReservationModality> MORNING_MODALITIES = List.of(
            ReservationModality.DIA_COMPLETO,
            ReservationModality.MANANA,
            ReservationModality.MEDIA_JORNADA
    );
    private static final List<ReservationModality> AFTERNOON_MODALITIES = List.of(
            ReservationModality.DIA_COMPLETO,
            ReservationModality.TARDE,
            ReservationModality.MEDIA_JORNADA
    );

    private final ReservationRepository reservationRepository;

    public ReservationCapacityService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Obtiene ocupación sin bloquear, apropiada para una consulta informativa.
     *
     * @param groupId grupo consultado
     * @param date fecha consultada
     * @return reservas activas que ocupan cada franja
     */
    public ReservationCapacitySnapshot findSnapshot(Long groupId, LocalDate date) {
        return findSnapshot(groupId, date, null);
    }

    /**
     * Obtiene ocupación excluyendo una reserva cuando se reasigna o confirma.
     *
     * @param groupId grupo consultado
     * @param date fecha consultada
     * @param excludedReservationId reserva excluida, o nula
     * @return reservas activas que ocupan cada franja
     */
    public ReservationCapacitySnapshot findSnapshot(
            Long groupId,
            LocalDate date,
            Long excludedReservationId
    ) {
        long morning = count(groupId, date, MORNING_MODALITIES, excludedReservationId);
        long afternoon = count(groupId, date, AFTERNOON_MODALITIES, excludedReservationId);
        return new ReservationCapacitySnapshot(morning, afternoon);
    }

    /**
     * Comprueba que la modalidad cabe en las franjas solicitadas.
     *
     * @param group grupo reservado
     * @param date fecha
     * @param modality modalidad
     * @param excludedReservationId reserva excluida, o nula
     */
    public void requireAvailability(
            DaycareGroupEntity group,
            LocalDate date,
            ReservationModality modality,
            Long excludedReservationId
    ) {
        ReservationCapacitySnapshot snapshot = findSnapshot(group.getId(), date, excludedReservationId);
        int capacity = group.getMaxCapacity();
        if (occupiesMorning(modality) && snapshot.morningOccupied() >= capacity) {
            throw new CapacityExceededException("No quedan plazas de mañana en el grupo seleccionado.");
        }
        if (occupiesAfternoon(modality) && snapshot.afternoonOccupied() >= capacity) {
            throw new CapacityExceededException("No quedan plazas de tarde en el grupo seleccionado.");
        }
    }

    private long count(
            Long groupId,
            LocalDate date,
            List<ReservationModality> modalities,
            Long excludedReservationId
    ) {
        return excludedReservationId == null
                ? this.reservationRepository.countByGroup_IdAndDateAndStatusNotAndModalityIn(
                        groupId, date, ReservationStatus.CANCELADA, modalities)
                : this.reservationRepository.countByGroup_IdAndDateAndStatusNotAndModalityInAndIdNot(
                        groupId, date, ReservationStatus.CANCELADA, modalities, excludedReservationId);
    }

    private boolean occupiesMorning(ReservationModality modality) {
        return MORNING_MODALITIES.contains(modality);
    }

    private boolean occupiesAfternoon(ReservationModality modality) {
        return AFTERNOON_MODALITIES.contains(modality);
    }
}
