package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.ReceptionReservationPageDto;
import com.campusdual.cd2026mnd1g1.model.entity.CenterEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationModality;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.TutorEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.CheckRecordRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AllergyRepository;
import com.campusdual.cd2026mnd1g1.model.repository.AuthorizedPersonRepository;
import com.campusdual.cd2026mnd1g1.model.repository.MedicationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceptionReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CheckRecordRepository checkRecordRepository;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private AuthorizedPersonRepository authorizedPersonRepository;

    private ReceptionReservationServiceImpl service;

    @BeforeEach
    void setUp() {
        this.service = new ReceptionReservationServiceImpl(
                this.userRepository,
                this.reservationRepository,
                this.checkRecordRepository,
                this.allergyRepository,
                this.medicationRepository,
                this.authorizedPersonRepository,
                Clock.fixed(Instant.parse("2026-11-16T23:30:00Z"), ZoneId.of("Europe/Madrid"))
        );
    }

    @Test
    void listUsesRequestedDateAndAuthenticatedCenterAndIncludesCancelledRows() {
        UserEntity user = userInCenter(17L);
        ReservationEntity reservation = reservation(21L, user.getCenter(), "Luna");
        reservation.setStatus(ReservationStatus.CANCELADA);
        when(this.userRepository.findByUsernameOrEmail("recepcionista", "recepcionista"))
                .thenReturn(Optional.of(user));
        when(this.reservationRepository.findByDateAndDog_Center_Id(eq(LocalDate.of(2026, 11, 20)), eq(17L), any()))
                .thenReturn(new PageImpl<>(List.of(reservation)));
        when(this.checkRecordRepository.findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(List.of(21L)))
                .thenReturn(List.of());

        ReceptionReservationPageDto page = this.service.findReservations(
                LocalDate.of(2026, 11, 20), 0, 50, "recepcionista"
        );

        assertEquals(1, page.totalElements());
        assertEquals("CANCELADA", page.items().get(0).status());
        assertEquals("SIN_ENTRADA_REGISTRADA", page.items().get(0).presenceStatus());
        verify(this.reservationRepository).findByDateAndDog_Center_Id(
                eq(LocalDate.of(2026, 11, 20)), eq(17L), any(Pageable.class)
        );
    }

    @Test
    void missingDateUsesTodayInEuropeMadridAndOrdersByNameThenId() {
        UserEntity user = userInCenter(17L);
        when(this.userRepository.findByUsernameOrEmail("recepcionista", "recepcionista"))
                .thenReturn(Optional.of(user));
        when(this.reservationRepository.findByDateAndDog_Center_Id(eq(LocalDate.of(2026, 11, 17)), eq(17L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        this.service.findReservations(null, 0, 50, "recepcionista");

        verify(this.reservationRepository).findByDateAndDog_Center_Id(
                eq(LocalDate.of(2026, 11, 17)), eq(17L), pageableCaptor.capture()
        );
        assertEquals("dog.name", pageableCaptor.getValue().getSort().getOrderFor("dog.name").getProperty());
        assertEquals("id", pageableCaptor.getValue().getSort().getOrderFor("id").getProperty());
    }

    @Test
    void presenceUsesCheckRecordsAndFlagsExitWithoutEntry() {
        UserEntity user = userInCenter(17L);
        ReservationEntity reservation = reservation(21L, user.getCenter(), "Bruno");
        when(this.userRepository.findByUsernameOrEmail("recepcionista", "recepcionista"))
                .thenReturn(Optional.of(user));
        when(this.reservationRepository.findByDateAndDog_Center_Id(eq(reservation.getDate()), eq(17L), any()))
                .thenReturn(new PageImpl<>(List.of(reservation)));
        when(this.reservationRepository.findByIdAndDog_Center_Id(21L, 17L))
                .thenReturn(Optional.of(reservation));
        when(this.checkRecordRepository.findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(List.of(21L)))
                .thenReturn(List.of(checkRecord(1L, reservation, CheckRecordType.SALIDA, "2026-11-17T10:00:00")));

        ReceptionReservationPageDto page = this.service.findReservations(
                reservation.getDate(), 0, 50, "recepcionista"
        );

        assertEquals("REGISTRO_INCOHERENTE", page.items().get(0).presenceStatus());
        var detail = this.service.findReservation(21L, "recepcionista");
        assertTrue(detail.presence().inconsistent());
        assertEquals("REGISTRO_INCOHERENTE", detail.presence().status());
    }

    @Test
    void presenceAfterEntryAndExitIsNotBasedOnReservationStatus() {
        UserEntity user = userInCenter(17L);
        ReservationEntity reservation = reservation(21L, user.getCenter(), "Coco");
        reservation.setStatus(ReservationStatus.PENDIENTE);
        when(this.userRepository.findByUsernameOrEmail("recepcionista", "recepcionista"))
                .thenReturn(Optional.of(user));
        when(this.reservationRepository.findByIdAndDog_Center_Id(21L, 17L))
                .thenReturn(Optional.of(reservation));
        when(this.checkRecordRepository.findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(List.of(21L)))
                .thenReturn(List.of(
                        checkRecord(1L, reservation, CheckRecordType.ENTRADA, "2026-11-17T09:00:00"),
                        checkRecord(2L, reservation, CheckRecordType.SALIDA, "2026-11-17T12:00:00")
                ));

        var detail = this.service.findReservation(21L, "recepcionista");

        assertEquals("SALIDA_REGISTRADA", detail.presence().status());
        assertFalse(detail.presence().inconsistent());
        assertEquals(ReservationStatus.PENDIENTE.name(), detail.status());
    }

    @Test
    void detailForAnotherCenterIsNotFound() {
        UserEntity user = userInCenter(17L);
        when(this.userRepository.findByUsernameOrEmail("recepcionista", "recepcionista"))
                .thenReturn(Optional.of(user));
        when(this.reservationRepository.findByIdAndDog_Center_Id(999L, 17L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> this.service.findReservation(999L, "recepcionista"));
    }

    @Test
    void invalidPageSizeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> this.service.findReservations(LocalDate.now(), 0, 101, "recepcionista")
        );
    }

    private UserEntity userInCenter(Long centerId) {
        CenterEntity center = new CenterEntity();
        center.setId(centerId);
        UserEntity user = new UserEntity();
        user.setCenter(center);
        user.setActive(true);
        return user;
    }

    private ReservationEntity reservation(Long id, CenterEntity center, String dogName) {
        DogEntity dog = new DogEntity();
        dog.setId(id + 100);
        dog.setCenter(center);
        dog.setName(dogName);
        dog.setSize(com.campusdual.cd2026mnd1g1.model.entity.DogSize.MEDIANO);
        TutorEntity tutor = new TutorEntity();
        tutor.setFirstName("Ana");
        tutor.setLastName("Núñez");
        tutor.setPhone("981000101");
        dog.setTutor(tutor);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(id);
        reservation.setDog(dog);
        reservation.setDate(LocalDate.of(2026, 11, 17));
        reservation.setModality(ReservationModality.DIA_COMPLETO);
        reservation.setStatus(ReservationStatus.CONFIRMADA);
        return reservation;
    }

    private CheckRecordEntity checkRecord(
            Long id,
            ReservationEntity reservation,
            CheckRecordType type,
            String occurredAt
    ) {
        CheckRecordEntity record = new CheckRecordEntity();
        record.setId(id);
        record.setReservation(reservation);
        record.setType(type);
        record.setOccurredAt(LocalDateTime.parse(occurredAt));
        record.setPetCondition("Buen estado");
        return record;
    }
}
