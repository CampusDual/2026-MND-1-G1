package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionReservationRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionAvailabilityDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionBookingCreatedDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionGroupOptionDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionBookingService;
import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationModality;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityConfigurationMissingException;
import com.campusdual.cd2026mnd1g1.model.exception.DuplicateReservationException;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ReservationDateNotAllowedException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareGroupRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona las reservas internas y consulta la disponibilidad del centro.
 */
@Service
@Transactional
public class ReceptionBookingServiceImpl implements ReceptionBookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceptionBookingServiceImpl.class);
    private final UserRepository userRepository;
    private final DogRepository dogRepository;
    private final DaycareGroupRepository groupRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationCapacityService capacityService;
    private final Clock receptionClock;

    public ReceptionBookingServiceImpl(
            UserRepository userRepository,
            DogRepository dogRepository,
            DaycareGroupRepository groupRepository,
            ReservationRepository reservationRepository,
            ReservationCapacityService capacityService,
            Clock receptionClock
    ) {
        this.userRepository = userRepository;
        this.dogRepository = dogRepository;
        this.groupRepository = groupRepository;
        this.reservationRepository = reservationRepository;
        this.capacityService = capacityService;
        this.receptionClock = receptionClock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceptionGroupOptionDto> findGroups(String username) {
        UserEntity user = findActiveUser(username);
        List<DaycareGroupEntity> groups = this.groupRepository
                .findByCenter_IdOrderByNameAsc(user.getCenter().getId());
        if (groups.isEmpty()) {
            LOGGER.warn("No hay grupos configurados para el centro {} al consultar reservas", user.getCenter().getId());
            throw new CapacityConfigurationMissingException(
                    "El centro no tiene grupos con capacidad configurada para reservar."
            );
        }
        return groups.stream()
                .map(group -> new ReceptionGroupOptionDto(
                        group.getId(),
                        group.getName(),
                        group.getMaxCapacity()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionAvailabilityDto findAvailability(LocalDate date, Long groupId, String username) {
        UserEntity user = findActiveUser(username);
        validateAllowedDate(date);
        validatePositiveId(groupId, "grupo");
        DaycareGroupEntity group = this.groupRepository
                .findByIdAndCenter_Id(groupId, user.getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el grupo indicado en el centro"));
        ReservationCapacitySnapshot snapshot = this.capacityService.findSnapshot(group.getId(), date);
        return toAvailability(date, group, snapshot);
    }

    @Override
    public ReceptionBookingCreatedDto createReservation(
            CreateReceptionReservationRequestDto request,
            String username
    ) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        UserEntity user = findActiveUser(username);
        validateAllowedDate(request.date());
        validatePositiveId(request.dogId(), "perro");
        validatePositiveId(request.groupId(), "grupo");
        ReservationModality modality = parseModality(request.modality());

        Long centerId = user.getCenter().getId();
        DogEntity dog = this.dogRepository.findByIdAndCenterIdForUpdate(request.dogId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el perro indicado en el centro"));
        validateReservationCandidate(dog, centerId);

        DaycareGroupEntity group = this.groupRepository
                .findByIdAndCenterIdForUpdate(request.groupId(), centerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el grupo indicado en el centro"));

        if (this.reservationRepository.existsByDog_IdAndDateAndStatusNot(
                dog.getId(),
                request.date(),
                ReservationStatus.CANCELADA
        )) {
            throw new DuplicateReservationException(
                    "El perro ya tiene una reserva activa para esa fecha."
            );
        }

        this.capacityService.requireAvailability(group, request.date(), modality, null);
        ReservationEntity reservation = new ReservationEntity();
        reservation.setDog(dog);
        reservation.setGroup(group);
        reservation.setDate(request.date());
        reservation.setModality(modality);
        reservation.setStatus(ReservationStatus.CONFIRMADA);
        reservation.setCreatedAt(LocalDateTime.now(this.receptionClock));
        ReservationEntity saved = this.reservationRepository.save(reservation);
        return new ReceptionBookingCreatedDto(
                saved.getId(),
                saved.getDate(),
                saved.getModality().name(),
                saved.getStatus().name(),
                dog.getId(),
                dog.getName(),
                group.getId(),
                group.getName()
        );
    }

    private ReceptionAvailabilityDto toAvailability(
            LocalDate date,
            DaycareGroupEntity group,
            ReservationCapacitySnapshot snapshot
    ) {
        long capacity = group.getMaxCapacity();
        return new ReceptionAvailabilityDto(
                date,
                group.getId(),
                group.getName(),
                group.getMaxCapacity(),
                snapshot.morningOccupied(),
                Math.max(0, capacity - snapshot.morningOccupied()),
                snapshot.afternoonOccupied(),
                Math.max(0, capacity - snapshot.afternoonOccupied())
        );
    }

    private void validateReservationCandidate(DogEntity dog, Long centerId) {
        if (!Boolean.TRUE.equals(dog.getActive())
                || dog.getTutor() == null
                || dog.getTutor().getCenter() == null
                || !centerId.equals(dog.getTutor().getCenter().getId())) {
            throw new ResourceNotFoundException("El perro no es reservable en este centro");
        }
    }

    private void validateAllowedDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (date.isBefore(LocalDate.now(this.receptionClock))) {
            throw new ReservationDateNotAllowedException("No se permiten reservas en fechas pasadas.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReservationDateNotAllowedException("El centro no admite reservas los domingos.");
        }
    }

    private ReservationModality parseModality(String value) {
        if (value == null) {
            throw new IllegalArgumentException("La modalidad es obligatoria");
        }
        if ("MEDIA_JORNADA".equals(value)) {
            throw new IllegalArgumentException("Selecciona mañana o tarde para una media jornada.");
        }
        try {
            return ReservationModality.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La modalidad de reserva no es válida");
        }
    }

    private UserEntity findActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new OperationalUserNotFoundException("No se pudo identificar al usuario autenticado");
        }
        UserEntity user = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un centro asociado"
                ));
        if (!Boolean.TRUE.equals(user.getActive()) || user.getCenter() == null) {
            throw new OperationalUserNotFoundException("El usuario operativo está desactivado o sin centro");
        }
        return user;
    }

    private void validatePositiveId(Long id, String entityName) {
        if (id == null || id < 0) {
            throw new IllegalArgumentException("El identificador de " + entityName + " no puede ser negativo");
        }
    }
}
