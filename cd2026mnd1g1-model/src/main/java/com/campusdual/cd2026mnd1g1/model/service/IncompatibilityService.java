package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncompatibilityRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class IncompatibilityService {

    private final DogRepository dogRepository;
    private final IncompatibilityRepository incompatibilityRepository;
    private final ReservationRepository reservationRepository;

    public IncompatibilityService(
            DogRepository dogRepository,
            IncompatibilityRepository incompatibilityRepository,
            ReservationRepository reservationRepository
    ) {
        this.dogRepository = dogRepository;
        this.incompatibilityRepository = incompatibilityRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Registra una incompatibilidad ordenando los perros por identificador.
     *
     * @param firstDogId identificador de un perro
     * @param secondDogId identificador del otro perro
     * @param reason motivo opcional de la incompatibilidad
     * @return incompatibilidad persistida
     */
    public IncompatibilityEntity create(Long firstDogId, Long secondDogId, String reason) {
        validateDogIds(firstDogId, secondDogId);

        Long dogAId = Math.min(firstDogId, secondDogId);
        Long dogBId = Math.max(firstDogId, secondDogId);
        DogEntity dogA = this.dogRepository.findById(dogAId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el primer perro indicado"));
        DogEntity dogB = this.dogRepository.findById(dogBId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el segundo perro indicado"));

        IncompatibilityEntity incompatibility = new IncompatibilityEntity();
        incompatibility.setFirstDog(dogA);
        incompatibility.setSecondDog(dogB);
        incompatibility.setReason(reason);
        return this.incompatibilityRepository.save(incompatibility);
    }

    /**
     * Busca los perros incompatibles ya reservados en el mismo grupo y fecha.
     *
     * @param dogId identificador del perro asignado
     * @param groupId identificador del grupo
     * @param date fecha de la reserva
     * @return identificadores de perros incompatibles presentes
     */
    @Transactional(readOnly = true)
    public List<Long> findConflictingDogIds(Long dogId, Long groupId, LocalDate date) {
        validateId(dogId, "perro");
        validateId(groupId, "grupo");
        if (date == null) {
            throw new IllegalArgumentException("La fecha de la reserva es obligatoria");
        }

        List<ReservationEntity> reservations = this.reservationRepository
                .findByDateAndGroup_IdAndStatusNot(date, groupId, ReservationStatus.CANCELADA);
        List<Long> conflicts = new ArrayList<>();
        for (ReservationEntity reservation : reservations) {
            Long existingDogId = reservation.getDog().getId();
            if (existingDogId == null || existingDogId.equals(dogId)) {
                continue;
            }
            Long dogAId = Math.min(dogId, existingDogId);
            Long dogBId = Math.max(dogId, existingDogId);
            if (this.incompatibilityRepository.existsBetweenDogs(dogAId, dogBId)) {
                conflicts.add(existingDogId);
            }
        }
        return List.copyOf(conflicts);
    }

    private void validateDogIds(Long firstDogId, Long secondDogId) {
        validateId(firstDogId, "perro");
        validateId(secondDogId, "perro");
        if (firstDogId.equals(secondDogId)) {
            throw new IllegalArgumentException("No se puede registrar un perro como incompatible consigo mismo");
        }
    }

    private void validateId(Long id, String entityName) {
        if (Objects.isNull(id) || id <= 0) {
            throw new IllegalArgumentException("El identificador de " + entityName + " debe ser mayor que cero");
        }
    }
}
