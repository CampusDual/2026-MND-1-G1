package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.VoucherEntity;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareGroupRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private DaycareGroupRepository groupRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private IncompatibilityService incompatibilityService;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void confirmRejectsReservationWhenGroupCapacityIsReached() {
        ReservationEntity reservation = reservationWithGroup(5, 2);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.reservationRepository.countByGroup_IdAndDateAndStatusNot(
                5L, reservation.getDate(), ReservationStatus.CANCELADA
        )).thenReturn(2L);

        assertThrows(IllegalStateException.class, () -> this.reservationService.confirm(1L));

        verify(this.reservationRepository, never()).save(any());
    }

    @Test
    void confirmConsumesOneVoucherDayWhenCapacityIsAvailable() {
        ReservationEntity reservation = reservationWithGroup(5, 3);
        VoucherEntity voucher = voucher(3, LocalDate.now().plusDays(1));
        reservation.setVoucher(voucher);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.reservationRepository.countByGroup_IdAndDateAndStatusNot(
                5L, reservation.getDate(), ReservationStatus.CANCELADA
        )).thenReturn(2L);
        when(this.reservationRepository.save(reservation)).thenReturn(reservation);
        when(this.voucherRepository.save(voucher)).thenReturn(voucher);

        this.reservationService.confirm(1L);
        this.reservationService.confirm(1L);

        assertEquals(2, voucher.getRemainingDays());
        assertEquals(ReservationStatus.CONFIRMADA, reservation.getStatus());
        verify(this.voucherRepository).save(voucher);
    }

    @Test
    void confirmRejectsExpiredVoucher() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setVoucher(voucher(2, LocalDate.now().minusDays(1)));
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.reservationRepository.countByGroup_IdAndDateAndStatusNot(
                5L, reservation.getDate(), ReservationStatus.CANCELADA
        )).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> this.reservationService.confirm(1L));
    }

    @Test
    void confirmRejectsVoucherWithoutRemainingDays() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setVoucher(voucher(0, null));
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.reservationRepository.countByGroup_IdAndDateAndStatusNot(
                5L, reservation.getDate(), ReservationStatus.CANCELADA
        )).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> this.reservationService.confirm(1L));
    }

    @Test
    void cancelReturnsConsumedVoucherDayOnlyOnce() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setStatus(ReservationStatus.CONFIRMADA);
        VoucherEntity voucher = voucher(4, null);
        reservation.setVoucher(voucher);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.reservationRepository.save(reservation)).thenReturn(reservation);
        when(this.voucherRepository.save(voucher)).thenReturn(voucher);

        this.reservationService.cancel(1L);
        this.reservationService.cancel(1L);

        assertEquals(5, voucher.getRemainingDays());
        assertEquals(ReservationStatus.CANCELADA, reservation.getStatus());
        verify(this.voucherRepository).save(voucher);
    }

    @Test
    void assignGroupReturnsWarningButPersistsAssignment() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setGroup(null);
        DaycareGroupEntity group = new DaycareGroupEntity();
        group.setId(5L);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.groupRepository.findById(5L)).thenReturn(Optional.of(group));
        when(this.incompatibilityService.findConflictingDogIds(
                10L, 5L, reservation.getDate()
        )).thenReturn(List.of(12L));
        when(this.reservationRepository.save(reservation)).thenReturn(reservation);

        List<String> warnings = this.reservationService.assignGroup(1L, 5L);

        assertEquals(group, reservation.getGroup());
        assertEquals(1, warnings.size());
        verify(this.reservationRepository).save(reservation);
    }

    private ReservationEntity reservationWithGroup(long groupId, int capacity) {
        DaycareGroupEntity group = new DaycareGroupEntity();
        group.setId(groupId);
        group.setMaxCapacity(capacity);
        DogEntity dog = new DogEntity();
        dog.setId(10L);

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(1L);
        reservation.setDog(dog);
        reservation.setGroup(group);
        reservation.setDate(LocalDate.now().plusDays(1));
        reservation.setStatus(ReservationStatus.PENDIENTE);
        return reservation;
    }

    private VoucherEntity voucher(int remainingDays, LocalDate expirationDate) {
        VoucherEntity voucher = new VoucherEntity();
        voucher.setId(3L);
        voucher.setTotalDays(5);
        voucher.setRemainingDays(remainingDays);
        voucher.setExpirationDate(expirationDate);
        return voucher;
    }
}
