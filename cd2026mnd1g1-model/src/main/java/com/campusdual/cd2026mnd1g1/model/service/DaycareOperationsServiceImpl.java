package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.AllergyDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.CheckRecordTypeDto;
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
import com.campusdual.cd2026mnd1g1.api.service.DaycareOperationsService;
import com.campusdual.cd2026mnd1g1.model.entity.AllergyEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DaycareUserEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentSeverity;
import com.campusdual.cd2026mnd1g1.model.entity.MedicationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.CheckRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DailyRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareUserRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncompatibilityRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncidentRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class DaycareOperationsServiceImpl implements DaycareOperationsService {

    private final DaycareUserRepository daycareUserRepository;
    private final ReservationRepository reservationRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final IncompatibilityRepository incompatibilityRepository;
    private final CheckRecordRepository checkRecordRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final IncidentRepository incidentRepository;

    public DaycareOperationsServiceImpl(
            DaycareUserRepository daycareUserRepository,
            ReservationRepository reservationRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            IncompatibilityRepository incompatibilityRepository,
            CheckRecordRepository checkRecordRepository,
            DailyRecordRepository dailyRecordRepository,
            IncidentRepository incidentRepository
    ) {
        this.daycareUserRepository = daycareUserRepository;
        this.reservationRepository = reservationRepository;
        this.allergyRepository = allergyRepository;
        this.medicationRepository = medicationRepository;
        this.incompatibilityRepository = incompatibilityRepository;
        this.checkRecordRepository = checkRecordRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDayDto> findReservations(LocalDate date, Long groupId, String username) {
        DaycareUserEntity user = findActiveUser(username);
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
        DaycareUserEntity user = findActiveUser(username);
        ReservationEntity reservation = findReservationInCenter(reservationId, user);
        LocalDateTime occurredAt = LocalDateTime.now();

        CheckRecordType type = CheckRecordType.valueOf(request.getType().name());
        reservation.setStatus(type == CheckRecordType.ENTRADA
                ? ReservationStatus.EN_CURSO
                : ReservationStatus.COMPLETADA);
        this.reservationRepository.save(reservation);

        CheckRecordEntity record = new CheckRecordEntity();
        record.setReservation(reservation);
        record.setUser(user);
        record.setType(type);
        record.setOccurredAt(occurredAt);
        record.setPetCondition(request.getPetCondition());
        record.setObservations(request.getObservations());
        return mapCheckRecord(this.checkRecordRepository.save(record));
    }

    @Override
    public DailyRecordDto createDailyRecord(Long reservationId, DailyRecordRequestDto request, String username) {
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("El tipo de actividad es obligatorio");
        }
        DaycareUserEntity user = findActiveUser(username);
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
        DaycareUserEntity user = findActiveUser(username);
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
                medication.getInstructions()
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
        return dto;
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

    private DaycareUserEntity findActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new OperationalUserNotFoundException("No se pudo identificar al usuario autenticado");
        }
        DaycareUserEntity user = this.daycareUserRepository.findByEmail(username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un usuario operativo asociado al centro"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new OperationalUserNotFoundException("El usuario operativo está desactivado");
        }
        return user;
    }

    private ReservationEntity findReservationInCenter(Long reservationId, DaycareUserEntity user) {
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
