package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogAllergyDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogIncompatibilityDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogMedicationDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDogSummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverTutorContactDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryDto;
import com.campusdual.cd2026mnd1g1.api.dto.CaregiverDailySummaryRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.IncidentSeverityDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordRequestDto;
import com.campusdual.cd2026mnd1g1.api.dto.DailyRecordTypeDto;
import com.campusdual.cd2026mnd1g1.api.service.CaregiverPortalService;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DailySummaryEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.IncidentSeverity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.CheckRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncompatibilityRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DailyRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DailySummaryRepository;
import com.campusdual.cd2026mnd1g1.model.repository.IncidentRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resuelve las asignaciones de un cuidador desde grupos y reservas reales.
 */
@Service
@Transactional(readOnly = true)
public class CaregiverPortalServiceImpl implements CaregiverPortalService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final CheckRecordRepository checkRecordRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final IncompatibilityRepository incompatibilityRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final IncidentRepository incidentRepository;
    private final Clock receptionClock;

    public CaregiverPortalServiceImpl(
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            CheckRecordRepository checkRecordRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            IncompatibilityRepository incompatibilityRepository,
            DailyRecordRepository dailyRecordRepository,
            DailySummaryRepository dailySummaryRepository,
            IncidentRepository incidentRepository,
            Clock receptionClock
    ) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.checkRecordRepository = checkRecordRepository;
        this.allergyRepository = allergyRepository;
        this.medicationRepository = medicationRepository;
        this.incompatibilityRepository = incompatibilityRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.dailySummaryRepository = dailySummaryRepository;
        this.incidentRepository = incidentRepository;
        this.receptionClock = receptionClock;
    }

    @Override
    public CaregiverDogPageDto findTodayDogs(String username, int page, int size) {
        validatePagination(page, size);
        UserEntity caregiver = findActiveCaregiver(username);
        LocalDate today = LocalDate.now(this.receptionClock);
        Page<ReservationEntity> reservations = this.reservationRepository.findCaregiverTodayReservations(
                caregiver.getId(),
                caregiver.getCenter().getId(),
                today,
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                ))
        );

        Map<Long, LocalDateTime> arrivals = findArrivalTimes(reservations.getContent());
        List<CaregiverDogSummaryDto> items = reservations.getContent().stream()
                .map(reservation -> mapSummary(reservation, arrivals.get(reservation.getId())))
                .toList();
        return new CaregiverDogPageDto(
                items,
                reservations.getNumber(),
                reservations.getSize(),
                reservations.getTotalElements(),
                reservations.getTotalPages()
        );
    }

    @Override
    public CaregiverDogDetailDto findTodayDog(Long reservationId, String username) {
        if (reservationId == null || reservationId < 0) {
            throw new IllegalArgumentException("El identificador de reserva no puede ser negativo");
        }
        UserEntity caregiver = findActiveCaregiver(username);
        ReservationEntity reservation = this.reservationRepository.findCaregiverTodayReservation(
                reservationId,
                caregiver.getId(),
                caregiver.getCenter().getId(),
                LocalDate.now(this.receptionClock)
        ).orElseThrow(() -> new ResourceNotFoundException("No existe una estancia asignada a este cuidador"));
        LocalDateTime arrivalAt = this.checkRecordRepository
                .findByReservation_IdAndType(reservationId, CheckRecordType.ENTRADA)
                .map(CheckRecordEntity::getOccurredAt)
                .orElse(null);
        return mapDetail(reservation, arrivalAt);
    }

    @Override
    public String findTodayDogPhotoReference(Long reservationId, String username) {
        CaregiverDogDetailDto detail = findTodayDog(reservationId, username);
        if (!detail.photoAvailable()) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        ReservationEntity reservation = findAuthorizedReservation(reservationId, username);
        String reference = reservation.getDog().getPhotoUrl();
        if (reference == null || reference.isBlank()) {
            throw new ResourceNotFoundException("No hay una foto disponible para este perro");
        }
        return reference;
    }

    @Override
    public List<DailyRecordDto> findTodayDailyRecords(Long reservationId, String username) {
        validateReservationId(reservationId);
        findAuthorizedReservation(reservationId, username);
        return this.dailyRecordRepository
                .findByReservation_IdOrderByOccurredAtDescIdDesc(reservationId)
                .stream()
                .map(this::mapDailyRecord)
                .toList();
    }

    @Override
    @Transactional
    public DailyRecordDto createTodayDailyRecord(
            Long reservationId,
            DailyRecordRequestDto request,
            String username
    ) {
        validateReservationId(reservationId);
        if (request == null || request.getType() == null) {
            throw new IllegalArgumentException("El tipo de cuidado es obligatorio");
        }
        if (request.getNotes() != null && request.getNotes().length() > 2000) {
            throw new IllegalArgumentException("Las observaciones no pueden superar 2000 caracteres");
        }
        ReservationEntity reservation = findAuthorizedReservation(reservationId, username);
        UserEntity caregiver = findActiveCaregiver(username);
        DailyRecordEntity record = new DailyRecordEntity();
        record.setReservation(reservation);
        record.setUser(caregiver);
        record.setType(DailyRecordType.valueOf(request.getType().name()));
        record.setOccurredAt(LocalDateTime.now(this.receptionClock));
        record.setNotes(normalizeOptionalNotes(request.getNotes()));
        return mapDailyRecord(this.dailyRecordRepository.save(record));
    }

    @Override
    public CaregiverDailySummaryDto findTodaySummary(Long reservationId, String username) {
        validateReservationId(reservationId);
        findAuthorizedReservation(reservationId, username);
        return this.dailySummaryRepository
                .findByReservation_IdAndRecipientAndStatus(reservationId, "TUTOR", "PUBLICADO")
                .map(this::mapDailySummary)
                .orElse(null);
    }

    @Override
    @Transactional
    public CaregiverDailySummaryDto publishTodaySummary(
            Long reservationId,
            CaregiverDailySummaryRequestDto request,
            String username
    ) {
        validateReservationId(reservationId);
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new IllegalArgumentException("El resumen diario es obligatorio");
        }
        if (request.text().length() > 4000) {
            throw new IllegalArgumentException("El resumen no puede superar 4000 caracteres");
        }
        ReservationEntity reservation = findAuthorizedReservation(reservationId, username);
        UserEntity caregiver = findActiveCaregiver(username);
        if (this.dailySummaryRepository.findByReservation_IdAndRecipientAndStatus(
                reservationId,
                "TUTOR",
                "PUBLICADO"
        ).isPresent()) {
            throw new IllegalStateException("Ya existe un resumen publicado para esta estancia");
        }
        LocalDateTime now = LocalDateTime.now(this.receptionClock);
        DailySummaryEntity summary = new DailySummaryEntity();
        summary.setReservation(reservation);
        summary.setRecipient("TUTOR");
        summary.setText(request.text().trim());
        summary.setCaregiver(caregiver);
        summary.setCenter(caregiver.getCenter());
        summary.setStatus("PUBLICADO");
        summary.setGeneratedAt(now);
        summary.setPublishedAt(now);
        return mapDailySummary(this.dailySummaryRepository.save(summary));
    }

    private CaregiverDailySummaryDto mapDailySummary(DailySummaryEntity summary) {
        return new CaregiverDailySummaryDto(
                summary.getId(),
                summary.getReservation().getId(),
                summary.getText(),
                summary.getStatus(),
                summary.getPublishedAt()
        );
    }

    @Override
    public List<IncidentDto> findTodayIncidents(Long reservationId, String username) {
        validateReservationId(reservationId);
        findAuthorizedReservation(reservationId, username);
        return this.incidentRepository
                .findByReservation_IdOrderByOccurredAtDescIdDesc(reservationId)
                .stream()
                .map(this::mapIncident)
                .toList();
    }

    @Override
    @Transactional
    public IncidentDto createTodayIncident(
            Long reservationId,
            IncidentRequestDto request,
            String username
    ) {
        validateReservationId(reservationId);
        if (request == null
                || request.getSeverity() == null
                || request.getDescription() == null
                || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("La gravedad y la descripción de la incidencia son obligatorias");
        }
        if (request.getDescription().length() > 2000) {
            throw new IllegalArgumentException("La descripción no puede superar 2000 caracteres");
        }
        ReservationEntity reservation = findAuthorizedReservation(reservationId, username);
        UserEntity caregiver = findActiveCaregiver(username);
        IncidentEntity incident = new IncidentEntity();
        incident.setReservation(reservation);
        incident.setUser(caregiver);
        incident.setSeverity(IncidentSeverity.valueOf(request.getSeverity().name()));
        incident.setDescription(request.getDescription().trim());
        incident.setOccurredAt(LocalDateTime.now(this.receptionClock));
        return mapIncident(this.incidentRepository.save(incident));
    }

    private IncidentDto mapIncident(IncidentEntity incident) {
        return new IncidentDto(
                incident.getId(),
                incident.getReservation().getId(),
                incident.getSeverity() == null
                        ? null
                        : IncidentSeverityDto.valueOf(incident.getSeverity().name()),
                incident.getDescription(),
                incident.getOccurredAt()
        );
    }

    private void validateReservationId(Long reservationId) {
        if (reservationId == null || reservationId < 0) {
            throw new IllegalArgumentException("El identificador de reserva no puede ser negativo");
        }
    }

    private String normalizeOptionalNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
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

    private ReservationEntity findAuthorizedReservation(Long reservationId, String username) {
        UserEntity caregiver = findActiveCaregiver(username);
        return this.reservationRepository.findCaregiverTodayReservation(
                reservationId,
                caregiver.getId(),
                caregiver.getCenter().getId(),
                LocalDate.now(this.receptionClock)
        ).orElseThrow(() -> new ResourceNotFoundException("No existe una estancia asignada a este cuidador"));
    }

    private UserEntity findActiveCaregiver(String username) {
        if (username == null || username.isBlank()) {
            throw new OperationalUserNotFoundException("No se pudo identificar al usuario autenticado");
        }
        UserEntity user = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new OperationalUserNotFoundException(
                        "El usuario autenticado no tiene un centro asociado"
                ));
        if (!Boolean.TRUE.equals(user.getActive())
                || user.getCenter() == null
                || user.getRole() == null
                || !"CUIDADOR".equals(user.getRole().getName())) {
            throw new OperationalUserNotFoundException("El usuario operativo no está autorizado como cuidador");
        }
        return user;
    }

    private Map<Long, LocalDateTime> findArrivalTimes(List<ReservationEntity> reservations) {
        List<Long> ids = reservations.stream().map(ReservationEntity::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return this.checkRecordRepository
                .findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(ids)
                .stream()
                .filter(record -> record.getType() == CheckRecordType.ENTRADA)
                .collect(Collectors.toMap(
                        record -> record.getReservation().getId(),
                        CheckRecordEntity::getOccurredAt,
                        (first, ignored) -> first
                ));
    }

    private CaregiverDogSummaryDto mapSummary(ReservationEntity reservation, LocalDateTime arrivalAt) {
        DogEntity dog = reservation.getDog();
        return new CaregiverDogSummaryDto(
                reservation.getId(),
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getSize().name(),
                dog.getPhotoUrl() != null && !dog.getPhotoUrl().isBlank(),
                arrivalAt,
                "EN_EL_CENTRO",
                reservation.getGroup() == null ? null : reservation.getGroup().getName()
        );
    }

    private CaregiverDogDetailDto mapDetail(ReservationEntity reservation, LocalDateTime arrivalAt) {
        DogEntity dog = reservation.getDog();
        List<CaregiverDogAllergyDto> allergies = this.allergyRepository
                .findByDog_IdOrderByIdAsc(dog.getId())
                .stream()
                .map(allergy -> new CaregiverDogAllergyDto(allergy.getDescription()))
                .toList();
        List<CaregiverDogMedicationDto> medications = this.medicationRepository
                .findByDog_IdOrderByIdAsc(dog.getId())
                .stream()
                .map(medication -> new CaregiverDogMedicationDto(
                        medication.getId(),
                        medication.getMedication(),
                        medication.getDosage(),
                        medication.getInstructions(),
                        Boolean.TRUE.equals(medication.getDeclaredByTutor())
                ))
                .toList();
        List<CaregiverDogIncompatibilityDto> incompatibilities = this.incompatibilityRepository
                .findByFirstDog_IdOrSecondDog_Id(dog.getId(), dog.getId())
                .stream()
                .map(incompatibility -> mapIncompatibility(incompatibility, dog.getId()))
                .toList();

        return new CaregiverDogDetailDto(
                reservation.getId(),
                dog.getId(),
                dog.getName(),
                dog.getBreed(),
                dog.getSize().name(),
                dog.getBirthDate(),
                dog.getTutor() == null
                        ? null
                        : new CaregiverTutorContactDto(
                                (dog.getTutor().getFirstName() + " "
                                        + dog.getTutor().getLastName()).trim(),
                                dog.getTutor().getPhone()
                        ),
                dog.getPhotoUrl() != null && !dog.getPhotoUrl().isBlank(),
                arrivalAt,
                "EN_EL_CENTRO",
                reservation.getGroup() == null ? null : reservation.getGroup().getName(),
                dog.getFeedingInstructions(),
                dog.getTemperament(),
                allergies,
                medications,
                incompatibilities
        );
    }

    private CaregiverDogIncompatibilityDto mapIncompatibility(
            IncompatibilityEntity incompatibility,
            Long dogId
    ) {
        boolean firstDog = incompatibility.getFirstDog().getId().equals(dogId);
        DogEntity other = firstDog ? incompatibility.getSecondDog() : incompatibility.getFirstDog();
        return new CaregiverDogIncompatibilityDto(other.getName(), incompatibility.getReason());
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("La página debe ser cero o mayor");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
    }
}
