package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.ReceptionDogDto;
import com.campusdual.cd2026mnd1g1.api.dto.AuthorizedPersonDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionDepartureDataDto;
import com.campusdual.cd2026mnd1g1.api.dto.MedicationDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionPresenceDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationDetailDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationPageDto;
import com.campusdual.cd2026mnd1g1.api.dto.ReceptionTutorDto;
import com.campusdual.cd2026mnd1g1.api.service.ReceptionReservationService;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.AllergyEntity;
import com.campusdual.cd2026mnd1g1.model.entity.AuthorizedPersonEntity;
import com.campusdual.cd2026mnd1g1.model.entity.MedicationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.OperationalUserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.CheckRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AuthorizedPersonRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de reservas visibles para recepción, limitadas al centro del usuario.
 */
@Service
@Transactional(readOnly = true)
public class ReceptionReservationServiceImpl implements ReceptionReservationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String NO_ENTRY = "SIN_ENTRADA_REGISTRADA";
    private static final String IN_CENTER = "EN_EL_CENTRO";
    private static final String EXIT_RECORDED = "SALIDA_REGISTRADA";
    private static final String INCONSISTENT = "REGISTRO_INCOHERENTE";

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final CheckRecordRepository checkRecordRepository;
    private final AllergyRepository allergyRepository;
    private final MedicationRepository medicationRepository;
    private final AuthorizedPersonRepository authorizedPersonRepository;
    private final Clock receptionClock;

    public ReceptionReservationServiceImpl(
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            CheckRecordRepository checkRecordRepository,
            AllergyRepository allergyRepository,
            MedicationRepository medicationRepository,
            AuthorizedPersonRepository authorizedPersonRepository,
            Clock receptionClock
    ) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.checkRecordRepository = checkRecordRepository;
        this.allergyRepository = allergyRepository;
        this.medicationRepository = medicationRepository;
        this.authorizedPersonRepository = authorizedPersonRepository;
        this.receptionClock = receptionClock;
    }

    @Override
    public ReceptionReservationPageDto findReservations(LocalDate date, int page, int size, String username) {
        validatePagination(page, size);
        UserEntity user = findActiveUser(username);
        LocalDate requestedDate = date == null ? LocalDate.now(this.receptionClock) : date;

        Page<ReservationEntity> reservationPage = this.reservationRepository.findByDateAndDog_Center_Id(
                requestedDate,
                user.getCenter().getId(),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.asc("dog.name").ignoreCase(),
                                Sort.Order.asc("id")
                        )
                )
        );
        Map<Long, PresenceState> presenceStates = findPresenceStates(
                reservationPage.getContent().stream().map(ReservationEntity::getId).toList()
        );
        List<ReceptionReservationDto> items = reservationPage.getContent().stream()
                .map(reservation -> mapReservation(reservation, presenceStates))
                .toList();

        return new ReceptionReservationPageDto(
                items,
                reservationPage.getNumber(),
                reservationPage.getSize(),
                reservationPage.getTotalElements(),
                reservationPage.getTotalPages()
        );
    }

    @Override
    public ReceptionReservationDetailDto findReservation(Long reservationId, String username) {
        if (reservationId == null || reservationId < 0) {
            throw new IllegalArgumentException("El identificador de reserva no puede ser negativo");
        }
        UserEntity user = findActiveUser(username);
        ReservationEntity reservation = this.reservationRepository
                .findByIdAndDog_Center_Id(reservationId, user.getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva indicada"));
        ensureTutorBelongsToCenter(reservation.getDog(), user);
        PresenceState presence = findPresenceStates(List.of(reservationId))
                .getOrDefault(reservationId, PresenceState.withoutRecords());
        DogEntity dog = reservation.getDog();
        ReceptionTutorDto tutor = dog.getTutor() == null
                ? null
                : new ReceptionTutorDto(
                        fullName(dog.getTutor().getFirstName(), dog.getTutor().getLastName()),
                        dog.getTutor().getPhone()
                );

        return new ReceptionReservationDetailDto(
                reservation.getId(),
                reservation.getDate(),
                reservation.getModality().name(),
                reservation.getStatus().name(),
                new ReceptionDogDto(
                        dog.getId(),
                        dog.getName(),
                        dog.getBreed(),
                        dog.getSize().name(),
                        dog.getBirthDate(),
                        dog.getTemperament(),
                        dog.getFeedingInstructions(),
                        this.allergyRepository.findByDog_Id(dog.getId()).stream()
                                .map(AllergyEntity::getDescription)
                                .toList(),
                        this.medicationRepository.findByDog_Id(dog.getId()).stream()
                                .map(this::mapMedication)
                                .toList()
                ),
                tutor,
                presence.toDto()
        );
    }

    @Override
    public ReceptionDepartureDataDto findDepartureData(Long reservationId, String username) {
        ReceptionReservationDetailDto detail = findReservation(reservationId, username);
        ReservationEntity reservation = this.reservationRepository
                .findByIdAndDog_Center_Id(reservationId, findActiveUser(username).getCenter().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva indicada"));
        List<AuthorizedPersonDto> authorizedPersons = reservation.getDog().getTutor() == null
                ? List.of()
                : this.authorizedPersonRepository
                        .findByTutor_IdOrderByNameAscIdAsc(reservation.getDog().getTutor().getId())
                        .stream()
                        .map(this::mapAuthorizedPerson)
                        .toList();
        return new ReceptionDepartureDataDto(
                detail.id(),
                detail.date(),
                detail.modality(),
                detail.status(),
                detail.dog().name(),
                reservation.getDog().getTutor() == null ? null : reservation.getDog().getTutor().getId(),
                detail.tutor() == null ? null : detail.tutor().name(),
                detail.presence().lastEntryAt(),
                detail.presence().entryObservations(),
                detail.presence().status(),
                authorizedPersons
        );
    }

    private AuthorizedPersonDto mapAuthorizedPerson(AuthorizedPersonEntity person) {
        return new AuthorizedPersonDto(person.getId(), person.getName());
    }

    private void ensureTutorBelongsToCenter(DogEntity dog, UserEntity user) {
        if (dog.getTutor() != null
                && !dog.getTutor().getCenter().getId().equals(user.getCenter().getId())) {
            throw new ResourceNotFoundException("No existe la reserva indicada");
        }
    }

    private MedicationDto mapMedication(MedicationEntity medication) {
        return new MedicationDto(
                medication.getId(),
                medication.getMedication(),
                medication.getDosage(),
                medication.getInstructions()
        );
    }

    private ReceptionReservationDto mapReservation(
            ReservationEntity reservation,
            Map<Long, PresenceState> presenceStates
    ) {
        DogEntity dog = reservation.getDog();
        String tutorName = dog.getTutor() == null
                ? null
                : fullName(dog.getTutor().getFirstName(), dog.getTutor().getLastName());
        PresenceState presence = presenceStates.getOrDefault(
                reservation.getId(),
                PresenceState.withoutRecords()
        );
        return new ReceptionReservationDto(
                reservation.getId(),
                reservation.getDate(),
                reservation.getModality().name(),
                reservation.getStatus().name(),
                dog.getId(),
                dog.getName(),
                tutorName,
                presence.status()
        );
    }

    private Map<Long, PresenceState> findPresenceStates(Collection<Long> reservationIds) {
        if (reservationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PresenceState> presenceStates = new HashMap<>();
        List<CheckRecordEntity> records = this.checkRecordRepository
                .findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(reservationIds);
        for (CheckRecordEntity record : records) {
            Long reservationId = record.getReservation().getId();
            PresenceState state = presenceStates.computeIfAbsent(reservationId, ignored -> new PresenceState());
            state.apply(record);
        }
        return presenceStates;
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

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("La página debe ser cero o mayor");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
    }

    private String fullName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }

    private static final class PresenceState {
        private String status = NO_ENTRY;
        private boolean inCenter;
        private boolean inconsistent;
        private CheckRecordEntity lastEntry;
        private CheckRecordEntity lastExit;

        private static PresenceState withoutRecords() {
            return new PresenceState();
        }

        private void apply(CheckRecordEntity record) {
            if (record.getType() == CheckRecordType.ENTRADA) {
                this.lastEntry = record;
                this.inCenter = true;
                this.status = IN_CENTER;
                return;
            }
            this.lastExit = record;
            if (this.inCenter) {
                this.inCenter = false;
                this.status = EXIT_RECORDED;
            } else {
                this.inconsistent = true;
                this.status = INCONSISTENT;
            }
        }

        private String status() {
            return this.status;
        }

        private ReceptionPresenceDto toDto() {
            return new ReceptionPresenceDto(
                    this.status,
                    this.lastEntry == null ? null : this.lastEntry.getOccurredAt(),
                    this.lastEntry == null ? null : this.lastEntry.getPetCondition(),
                    this.lastEntry == null ? null : this.lastEntry.getObservations(),
                    this.lastEntry == null
                            ? null
                            : (this.lastEntry.getUser().getFirstName() + " "
                                    + this.lastEntry.getUser().getLastName()).trim(),
                    this.lastExit == null ? null : this.lastExit.getOccurredAt(),
                    this.lastExit == null
                            ? null
                            : (this.lastExit.getUser().getFirstName() + " "
                                    + this.lastExit.getUser().getLastName()).trim(),
                    this.lastExit == null || this.lastExit.getCollectedByTutor() == null
                            && this.lastExit.getCollectedByAuthorizedPerson() == null
                            ? null
                            : this.lastExit.getCollectedByTutor() != null
                                    ? "TUTOR"
                                    : "PERSONA_AUTORIZADA",
                    this.lastExit == null ? null : this.lastExit.getCollectorName(),
                    this.lastExit == null ? null : this.lastExit.getObservations(),
                    this.inconsistent
            );
        }
    }
}
