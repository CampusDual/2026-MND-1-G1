package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CenterEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.VoucherEntity;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityExceededException;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareGroupRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private DaycareGroupRepository groupRepository;

    @Mock
    private DogRepository dogRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private IncompatibilityService incompatibilityService;

    @Mock
    private ReservationCapacityService capacityService;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void confirmRejectsReservationWhenGroupCapacityIsReached() {
        ReservationEntity reservation = reservationWithGroup(5, 2);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        whenLockedGroup(reservation);
        doThrow(new CapacityExceededException("aforo completo")).when(this.capacityService)
                .requireAvailability(reservation.getGroup(), reservation.getDate(), reservation.getModality(), 1L);

        assertThrows(CapacityExceededException.class, () -> this.reservationService.confirm(1L));

        verify(this.reservationRepository, never()).save(any());
    }

    @Test
    void confirmConsumesOneVoucherDayWhenCapacityIsAvailable() {
        ReservationEntity reservation = reservationWithGroup(5, 3);
        VoucherEntity voucher = voucher(3, LocalDate.now().plusDays(1));
        reservation.setVoucher(voucher);
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        whenLockedGroup(reservation);
        when(this.reservationRepository.save(reservation)).thenReturn(reservation);
        when(this.voucherRepository.save(voucher)).thenReturn(voucher);

        this.reservationService.confirm(1L);
        this.reservationService.confirm(1L);

        assertEquals(2, voucher.getRemainingDays());
        assertEquals(ReservationStatus.CONFIRMADA, reservation.getStatus());
        verify(this.voucherRepository).save(voucher);
        verify(this.groupRepository).findByIdAndCenterIdForUpdate(5L, 1L);
    }

    @Test
    void confirmRejectsExpiredVoucher() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setVoucher(voucher(2, LocalDate.now().minusDays(1)));
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        whenLockedGroup(reservation);
        assertThrows(IllegalStateException.class, () -> this.reservationService.confirm(1L));
    }

    @Test
    void confirmRejectsVoucherWithoutRemainingDays() {
        ReservationEntity reservation = reservationWithGroup(5, 5);
        reservation.setVoucher(voucher(0, null));
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        whenLockedGroup(reservation);
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
        DogEntity dog = reservation.getDog();
        DaycareGroupEntity group = new DaycareGroupEntity();
        group.setId(5L);
        group.setCenter(dog.getCenter());
        when(this.reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(this.dogRepository.findByIdAndCenterIdForUpdate(10L, 1L)).thenReturn(Optional.of(dog));
        when(this.groupRepository.findByIdAndCenterIdForUpdate(5L, 1L)).thenReturn(Optional.of(group));
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
        CenterEntity center = new CenterEntity();
        center.setId(1L);
        group.setCenter(center);
        DogEntity dog = new DogEntity();
        dog.setId(10L);
        dog.setCenter(center);

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

    private void whenLockedGroup(ReservationEntity reservation) {
        when(this.groupRepository.findByIdAndCenterIdForUpdate(
                reservation.getGroup().getId(),
                reservation.getDog().getCenter().getId()
        )).thenReturn(Optional.of(reservation.getGroup()));
    }
}
