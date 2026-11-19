package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.AllergyDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordTypeDto;
import com.campusdual.cd2026mnd1g1.api.dto.CollectorTypeDto;
import com.campusdual.cd2026mnd1g1.api.dto.CreateReceptionArrivalRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordTypeDto;
import com.campusdual.cd2026mnd1g1.api.dto.DogDailyDto;
import com.campusdual.cd2026mnd1g1.api.dto.GroupOccupancyDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncompatibilityDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentSeverityDto;
import com.campusdual.cd2026mnd1g1.api.dto.MedicationDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReservationDayDto;
import com.campusdual.cd2026mnd1g1.api.dto.RegisterReceptionDepartureRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.DaycareOperationsService;
import com.campusdual.cd2026mnd1g1.model.entity.AllergyEntity;
import com.campusdual.cd2026mnd1g1.model.entity.AuthorizedPersonEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentSeverity;
import com.campusdual.cd2026mnd1g1.model.entity.MedicationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.CheckRecordConflictException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ReservationDateNotAllowedException;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AuthorizedPersonRepository;
import com.campusdual.cd2026mnd1g1.model.repository.CheckRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DailyRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncompatibilityRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncidentRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class DaycareOperationsServiceImpl implements DaycareOperationsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaycareOperationsServiceImpl.class);
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final IncompatibilityRepository incompatibilityRepository;
    private final CheckRecordRepository checkRecordRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final IncidentRepository incidentRepository;
    private final AuthorizedPersonRepository authorizedPersonRepository;
    private final Clock receptionClock;

    public DaycareOperationsServiceImpl(
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            IncompatibilityRepository incompatibilityRepository,
            CheckRecordRepository checkRecordRepository,
            DailyRecordRepository dailyRecordRepository,
            IncidentRepository incidentRepository,
            AuthorizedPersonRepository authorizedPersonRepository,
            Clock receptionClock
    ) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.allergyRepository = allergyRepository;
        this.medicationRepository = medicationRepository;
        this.incompatibilityRepository = incompatibilityRepository;
        this.checkRecordRepository = checkRecordRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.incidentRepository = incidentRepository;
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.receptionClock = receptionClock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDayDto> findReservations(LocalDate date, Long groupId, String username) {
        UserEntity user = findActiveUser(username);
        LocalDate requestedDate = date == null ? LocalDate.now() : date;
        validateOptionalId(groupId, "grupo");

        List<ReservationEntity> reservations = groupId == null
                ? this.reservationRepository.findByDateAndDog_Center_Id(requestedDate, user.getCenter().getId())
                : this.reservationRepository.findByDateAndDog_Center_IdAndGroup_Id(
                        requestedDate, user.getCenter().getId(), groupId);
        return reservations.stream().map(this::mapReservation).toList();
    }

    @Override
    public CheckRecordDto createCheckRecord(Long reservationId, CheckRecordRequestDto request, String username) {
        validateCheckRecordRequest(request);
        if (request.getType() == CheckRecordTypeDto.SALIDA) {
            throw new CheckRecordConflictException(
                    "DEPARTURE_ENDPOINT_REQUIRED",
                    "Registra la salida mediante el flujo de entrega de recepción."
            );
        }
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = lockReservationInCenter(reservationId, user);
        return createArrivalRecord(
                reservation,
                request.getObservations(),
                request.getPetCondition(),
                user
        );
    }

    @Override
    public CheckRecordDto registerArrival(
            Long reservationId,
            CreateReceptionArrivalRequestDto request,
            String username
    ) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        if (request.observations() != null && request.observations().length() > 2000) {
            throw new IllegalArgumentException("Las observaciones de llegada no pueden superar 2000 caracteres");
        }
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = lockReservationInCenter(reservationId, user);
        return createArrivalRecord(reservation, request.observations(), null, user);
    }

    @Override
    public CheckRecordDto registerDeparture(
            Long reservationId,
            RegisterReceptionDepartureRequestDto request,
            String username
    ) {
        validateDepartureRequest(request);
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = lockReservationInCenter(reservationId, user);
        if (reservation.getStatus() != ReservationStatus.EN_CURSO) {
            throw new CheckRecordConflictException(
                    "RESERVATION_NOT_ELIGIBLE",
                    "La reserva no figura como estancia en curso."
            );
        }

        Long id = reservation.getId();
        if (reservation.getDog().getTutor() != null
                && !reservation.getDog().getTutor().getCenter().getId().equals(user.getCenter().getId())) {
            throw new CheckRecordConflictException(
                    "INCONSISTENT_TUTOR_SCOPE",
                    "El tutor asociado no pertenece al centro de esta reserva."
            );
        }
        CheckRecordEntity entry = this.checkRecordRepository
                .findByReservation_IdAndType(id, CheckRecordType.ENTRADA)
                .orElseThrow(() -> new CheckRecordConflictException(
                        "INCONSISTENT_CHECK_RECORD",
                        "No existe una entrada registrada para esta estancia."
                ));
        if (this.checkRecordRepository.existsByReservation_IdAndType(id, CheckRecordType.SALIDA)) {
            throw new CheckRecordConflictException(
                    "DEPARTURE_ALREADY_RECORDED",
                    "La salida de esta reserva ya está registrada."
            );
        }

        LocalDateTime departureAt = LocalDateTime.now(this.receptionClock);
        if (entry.getOccurredAt().isAfter(departureAt)) {
            LOGGER.error("Entrada posterior a la hora actual en la reserva {}", id);
            throw new CheckRecordConflictException(
                    "INCONSISTENT_CHECK_RECORD",
                    "La hora de entrada es posterior a la hora actual; no se puede registrar la salida."
            );
        }

        CheckRecordEntity departure = new CheckRecordEntity();
        departure.setReservation(reservation);
        departure.setUser(user);
        departure.setType(CheckRecordType.SALIDA);
        departure.setOccurredAt(departureAt);
        departure.setObservations(normalizeDepartureObservations(request.observations()));
        if (request.collectorType() == CollectorTypeDto.TUTOR) {
            if (request.authorizedPersonId() != null || reservation.getDog().getTutor() == null) {
                throw new CheckRecordConflictException(
                        "INVALID_COLLECTOR",
                        "El tutor vinculado a esta reserva no está disponible como persona que recoge."
                );
            }
            departure.setCollectedByTutor(reservation.getDog().getTutor());
            departure.setCollectorName((reservation.getDog().getTutor().getFirstName() + " "
                    + reservation.getDog().getTutor().getLastName()).trim());
        } else {
            if (request.authorizedPersonId() == null || reservation.getDog().getTutor() == null) {
                throw new CheckRecordConflictException(
                        "INVALID_COLLECTOR",
                        "Selecciona una persona autorizada del tutor vinculado a esta reserva."
                );
            }
            AuthorizedPersonEntity person = this.authorizedPersonRepository
                    .findByIdAndTutorIdForUpdate(
                            request.authorizedPersonId(),
                            reservation.getDog().getTutor().getId()
                    )
                    .orElseThrow(() -> new CheckRecordConflictException(
                            "COLLECTOR_NOT_AUTHORIZED",
                            "La persona seleccionada no está autorizada por el tutor de esta reserva."
                    ));
            departure.setCollectedByAuthorizedPerson(person);
            departure.setCollectorName(person.getName());
        }

        reservation.setStatus(ReservationStatus.COMPLETADA);
        this.reservationRepository.saveAndFlush(reservation);
        return mapCheckRecord(this.checkRecordRepository.save(departure));
    }

    @Override
    public DailyRecordDto createDailyRecord(Long reservationId, DailyRecordRequestDto request, String username) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("El tipo de actividad es obligatorio");
        }
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = findReservationInCenter(reservationId, user);

        DailyRecordEntity record = new DailyRecordEntity();
        record.setReservation(reservation);
        record.setUser(user);
        record.setType(DailyRecordType.valueOf(request.getType().name()));
        record.setOccurredAt(LocalDateTime.now());
        record.setNotes(request.getNotes());
        return mapDailyRecord(this.dailyRecordRepository.save(record));
    }

    @Override
    public IncidentDto createIncident(Long reservationId, IncidentRequestDto request, String username) {
        if (request == null || request.getSeverity() == null
                || request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("La gravedad y la descripción de la incidencia son obligatorias");
        }
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = findReservationInCenter(reservationId, user);

        IncidentEntity incident = new IncidentEntity();
        incident.setReservation(reservation);
        incident.setUser(user);
        incident.setSeverity(IncidentSeverity.valueOf(request.getSeverity().name()));
        incident.setDescription(request.getDescription().trim());
        incident.setOccurredAt(LocalDateTime.now());
        return mapIncident(this.incidentRepository.save(incident));
    }

    private ReservationDayDto mapReservation(ReservationEntity reservation) {
        ReservationDayDto dto = new ReservationDayDto();
        dto.setId(reservation.getId());
        dto.setDate(reservation.getDate());
        dto.setModality(reservation.getModality().name());
        dto.setStatus(reservation.getStatus().name());
        dto.setDog(mapDog(reservation.getDog()));
        dto.setGroupOccupancy(mapGroupOccupancy(reservation.getGroup(), reservation.getDate()));
        return dto;
    }

    private DogDailyDto mapDog(DogEntity dog) {
        DogDailyDto dto = new DogDailyDto();
        dto.setId(dog.getId());
        dto.setName(dog.getName());
        dto.setBreed(dog.getBreed());
        dto.setSize(dog.getSize().name());
        dto.setBirthDate(dog.getBirthDate());

        List<AllergyDto> allergies = this.allergyRepository.findByDog_Id(dog.getId()).stream()
                .map(this::mapAllergy)
                .toList();
        List<MedicationDto> medications = this.medicationRepository.findByDog_Id(dog.getId()).stream()
                .map(this::mapMedication)
                .toList();
        List<IncompatibilityDto> incompatibilities = this.incompatibilityRepository
                .findByFirstDog_IdOrSecondDog_Id(dog.getId(), dog.getId()).stream()
                .map(incompatibility -> mapIncompatibility(incompatibility, dog.getId()))
                .toList();
        dto.setAllergies(allergies);
        dto.setMedications(medications);
        dto.setIncompatibilities(incompatibilities);
        return dto;
    }

    private GroupOccupancyDto mapGroupOccupancy(DaycareGroupEntity group, LocalDate date) {
        if (group == null) {
            return null;
        }
        long occupied = this.reservationRepository.countByGroup_IdAndDateAndStatusNot(
                group.getId(), date, ReservationStatus.CANCELADA);
        return new GroupOccupancyDto(group.getId(), occupied, group.getMaxCapacity());
    }

    private AllergyDto mapAllergy(AllergyEntity allergy) {
        return new AllergyDto(allergy.getId(), allergy.getDescription());
    }

    private MedicationDto mapMedication(MedicationEntity medication) {
        return new MedicationDto(
                medication.getId(),
                medication.getMedication(),
                medication.getDosage(),
                medication.getInstructions(),
                Boolean.TRUE.equals(medication.getDeclaredByTutor())
        );
    }

    private IncompatibilityDto mapIncompatibility(IncompatibilityEntity incompatibility, Long dogId) {
        DogEntity incompatibleDog = incompatibility.getFirstDog().getId().equals(dogId)
                ? incompatibility.getSecondDog()
                : incompatibility.getFirstDog();
        return new IncompatibilityDto(
                incompatibility.getId(),
                incompatibleDog.getId(),
                incompatibleDog.getName(),
                incompatibility.getReason()
        );
    }

    private CheckRecordDto mapCheckRecord(CheckRecordEntity record) {
        CheckRecordDto dto = new CheckRecordDto();
        dto.setId(record.getId());
        dto.setReservationId(record.getReservation().getId());
        dto.setType(CheckRecordTypeDto.valueOf(record.getType().name()));
        dto.setDateTime(record.getOccurredAt());
        dto.setPetCondition(record.getPetCondition());
        dto.setObservations(record.getObservations());
        dto.setReservationStatus(record.getReservation().getStatus().name());
        dto.setCollectorType(record.getCollectedByTutor() != null
                ? CollectorTypeDto.TUTOR.name()
                : record.getCollectedByAuthorizedPerson() != null
                        ? CollectorTypeDto.PERSONA_AUTORIZADA.name()
                        : null);
        dto.setCollectorName(record.getCollectorName());
        return dto;
    }

    private CheckRecordDto createArrivalRecord(
            ReservationEntity reservation,
            String observations,
            String petCondition,
            UserEntity user
    ) {
        LocalDate today = LocalDate.now(this.receptionClock);
        if (!today.equals(reservation.getDate())) {
            throw new ReservationDateNotAllowedException(
                    "Solo se puede registrar la llegada de una reserva de hoy."
            );
        }
        Long reservationId = reservation.getId();
        boolean hasEntry = this.checkRecordRepository.existsByReservation_IdAndType(
                reservationId,
                CheckRecordType.ENTRADA
        );
        boolean hasExit = this.checkRecordRepository.existsByReservation_IdAndType(
                reservationId,
                CheckRecordType.SALIDA
        );
        if (hasExit && !hasEntry) {
            LOGGER.error("Salida sin entrada detectada al registrar llegada para la reserva {}", reservationId);
            throw new CheckRecordConflictException(
                    "INCONSISTENT_CHECK_RECORD",
                    "La reserva tiene una salida sin entrada previa. Revisa el registro antes de continuar."
            );
        }
        if (hasEntry || hasExit) {
            throw new CheckRecordConflictException(
                    "ARRIVAL_ALREADY_RECORDED",
                    "La entrada de esta reserva ya está registrada."
            );
        }
        if (reservation.getStatus() == ReservationStatus.EN_CURSO) {
            LOGGER.error("Reserva {} en curso sin registro de entrada", reservationId);
            throw new CheckRecordConflictException(
                    "INCONSISTENT_CHECK_RECORD",
                    "La reserva figura en curso, pero no tiene una entrada registrada."
            );
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            throw new CheckRecordConflictException(
                    "RESERVATION_NOT_ELIGIBLE",
                    "Solo se puede registrar la llegada de una reserva confirmada."
            );
        }
        reservation.setStatus(ReservationStatus.EN_CURSO);
        this.reservationRepository.save(reservation);
        return saveCheckRecord(
                reservation,
                user,
                CheckRecordType.ENTRADA,
                LocalDateTime.now(this.receptionClock),
                petCondition,
                observations
        );
    }

    private CheckRecordDto saveCheckRecord(
            ReservationEntity reservation,
            UserEntity user,
            CheckRecordType type,
            LocalDateTime occurredAt,
            String petCondition,
            String observations
    ) {
        CheckRecordEntity record = new CheckRecordEntity();
        record.setReservation(reservation);
        record.setUser(user);
        record.setType(type);
        record.setOccurredAt(occurredAt);
        record.setPetCondition(petCondition);
        record.setObservations(observations);
        return mapCheckRecord(this.checkRecordRepository.save(record));
    }

    private void validateDepartureRequest(RegisterReceptionDepartureRequestDto request) {
        if (request == null || request.collectorType() == null) {
            throw new IllegalArgumentException("Debe indicarse quién recoge al perro");
        }
        validateTextLength(request.observations(), 2000, "Las observaciones");
    }

    private void validateTextLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " no puede superar " + maxLength + " caracteres");
        }
    }

    private String normalizeDepartureObservations(String observations) {
        if (observations == null || observations.isBlank()) {
            return null;
        }
        return observations.trim();
    }

    private DailyRecordDto mapDailyRecord(DailyRecordEntity record) {
        DailyRecordDto dto = new DailyRecordDto();
        dto.setId(record.getId());
        dto.setReservationId(record.getReservation().getId());
        dto.setType(DailyRecordTypeDto.valueOf(record.getType().name()));
        dto.setDateTime(record.getOccurredAt());
        dto.setNotes(record.getNotes());
        return dto;
    }

    private IncidentDto mapIncident(IncidentEntity incident) {
        IncidentDto dto = new IncidentDto();
        dto.setId(incident.getId());
        dto.setReservationId(incident.getReservation().getId());
        dto.setSeverity(IncidentSeverityDto.valueOf(incident.getSeverity().name()));
        dto.setDescription(incident.getDescription());
        dto.setDateTime(incident.getOccurredAt());
        return dto;
    }

    private UserEntity findActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new OperationalUserNotFoundException("No se pudo identificar al usuario autenticado");
        }
        UserEntity user = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un usuario operativo asociado al centro"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new OperationalUserNotFoundException("El usuario operativo está desactivado");
        }
        return user;
    }

    private ReservationEntity findReservationInCenter(Long reservationId, UserEntity user) {
        if (Objects.isNull(reservationId) || reservationId <= 0) {
            throw new IllegalArgumentException("El identificador de reserva debe ser mayor que cero");
        }
        ReservationEntity reservation = this.reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva indicada"));
        if (reservation.getDog().getCenter() == null
                || !reservation.getDog().getCenter().getId().equals(user.getCenter().getId())) {
            throw new ResourceNotFoundException("No existe la reserva indicada en el centro del usuario");
        }
        return reservation;
    }

    private ReservationEntity lockReservationInCenter(Long reservationId, UserEntity user) {
        if (reservationId == null || reservationId < 0) {
            throw new IllegalArgumentException("El identificador de reserva no puede ser negativo");
        }
        if (user.getCenter() == null) {
            throw new OperationalUserNotFoundException("El usuario operativo no tiene un centro asociado");
        }
        return this.reservationRepository.findByIdAndCenterIdForUpdate(
                        reservationId,
                        user.getCenter().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la reserva indicada en el centro del usuario"
                ));
    }

    private void validateCheckRecordRequest(CheckRecordRequestDto request) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("El tipo de registro es obligatorio");
        }
        if (request.getPetCondition() != null && request.getPetCondition().length() > 500) {
            throw new IllegalArgumentException("El estado de la mascota no puede superar 500 caracteres");
        }
        if (request.getObservations() != null && request.getObservations().length() > 2000) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 2000 caracteres");
        }
    }

    private void validateOptionalId(Long id, String entityName) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El identificador de " + entityName + " debe ser mayor que cero");
        }
    }
}
