package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.VoucherEntity;
import com.campusdual.cd2026mnd1g1.model.exception.CapacityConfigurationMissingException;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareGroupRepository;
import com.campusdual.cd2026mnd1g1.model.repository.DogRepository;
import com.campusdual.cd2026mnd1g1.model.repository.ReservationRepository;
import com.campusdual.cd2026mnd1g1.model.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final DaycareGroupRepository groupRepository;
    private final DogRepository dogRepository;
    private final VoucherRepository voucherRepository;
    private final IncompatibilityService incompatibilityService;
    private final ReservationCapacityService capacityService;

    public ReservationService(
            ReservationRepository reservationRepository,
            DaycareGroupRepository groupRepository,
            DogRepository dogRepository,
            VoucherRepository voucherRepository,
            IncompatibilityService incompatibilityService,
            ReservationCapacityService capacityService
    ) {
        this.reservationRepository = reservationRepository;
        this.groupRepository = groupRepository;
        this.dogRepository = dogRepository;
        this.voucherRepository = voucherRepository;
        this.incompatibilityService = incompatibilityService;
        this.capacityService = capacityService;
    }

    /**
     * Confirma una reserva si hay aforo y, si corresponde, consume un día del bono.
     *
     * @param reservationId identificador de la reserva
     * @return reserva confirmada
     */
    public ReservationEntity confirm(Long reservationId) {
        ReservationEntity reservation = findReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMADA) {
            return reservation;
        }
        if (reservation.getStatus() != ReservationStatus.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar reservas pendientes");
        }

        validateCapacity(reservation);
        consumeVoucherDay(reservation.getVoucher());
        reservation.setStatus(ReservationStatus.CONFIRMADA);
        return this.reservationRepository.save(reservation);
    }

    /**
     * Asigna un grupo y devuelve avisos de incompatibilidades sin impedir la asignación.
     *
     * @param reservationId identificador de la reserva
     * @param groupId identificador del grupo
     * @return avisos de perros incompatibles en el grupo y fecha
     */
    public List<String> assignGroup(Long reservationId, Long groupId) {
        ReservationEntity reservation = findReservation(reservationId);
        Long centerId = reservation.getDog().getCenter().getId();
        DogEntity dog = this.dogRepository.findByIdAndCenterIdForUpdate(
                        reservation.getDog().getId(),
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe el perro de la reserva"));
        DaycareGroupEntity group = this.groupRepository.findByIdAndCenterIdForUpdate(
                        validateId(groupId, "grupo"),
                        centerId
                )
                .orElseThrow(() -> new ResourceNotFoundException("No existe el grupo indicado en el centro"));
        if (reservation.getStatus() != ReservationStatus.CANCELADA) {
            this.capacityService.requireAvailability(
                    group,
                    reservation.getDate(),
                    reservation.getModality(),
                    reservation.getId()
            );
        }

        List<Long> incompatibleDogIds = this.incompatibilityService.findConflictingDogIds(
                dog.getId(),
                group.getId(),
                reservation.getDate()
        );

        reservation.setDog(dog);
        reservation.setGroup(group);
        this.reservationRepository.save(reservation);

        List<String> warnings = new ArrayList<>();
        for (Long incompatibleDogId : incompatibleDogIds) {
            warnings.add("El perro " + reservation.getDog().getId()
                    + " es incompatible con el perro " + incompatibleDogId
                    + " en el grupo " + group.getId()
                    + " para la fecha " + reservation.getDate());
        }
        return List.copyOf(warnings);
    }

    /**
     * Cancela una reserva y devuelve el día consumido si ya había sido confirmada.
     *
     * @param reservationId identificador de la reserva
     * @return reserva cancelada
     */
    public ReservationEntity cancel(Long reservationId) {
        ReservationEntity reservation = findReservation(reservationId);
        if (reservation.getStatus() == ReservationStatus.CANCELADA) {
            return reservation;
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETADA) {
            throw new IllegalStateException("No se puede cancelar una reserva completada");
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMADA
                || reservation.getStatus() == ReservationStatus.EN_CURSO) {
            restoreVoucherDay(reservation.getVoucher());
        }
        reservation.setStatus(ReservationStatus.CANCELADA);
        return this.reservationRepository.save(reservation);
    }

    private void validateCapacity(ReservationEntity reservation) {
        DaycareGroupEntity assignedGroup = reservation.getGroup();
        if (assignedGroup == null) {
            throw new CapacityConfigurationMissingException(
                    "La reserva necesita un grupo con capacidad configurada para confirmarse."
            );
        }
        Long centerId = reservation.getDog().getCenter().getId();
        DaycareGroupEntity group = this.groupRepository.findByIdAndCenterIdForUpdate(
                        assignedGroup.getId(),
                        centerId
                )
                .orElseThrow(() -> new CapacityConfigurationMissingException(
                        "La reserva necesita un grupo con capacidad configurada para confirmarse."
                ));
        reservation.setGroup(group);
        this.capacityService.requireAvailability(
                group,
                reservation.getDate(),
                reservation.getModality(),
                reservation.getId()
        );
    }

    private void consumeVoucherDay(VoucherEntity voucher) {
        if (voucher == null) {
            return;
        }
        if (voucher.getExpirationDate() != null && voucher.getExpirationDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("El bono está caducado");
        }
        if (voucher.getRemainingDays() == null || voucher.getRemainingDays() <= 0) {
            throw new IllegalStateException("El bono no tiene días disponibles");
        }
        voucher.setRemainingDays(voucher.getRemainingDays() - 1);
        this.voucherRepository.save(voucher);
    }

    private void restoreVoucherDay(VoucherEntity voucher) {
        if (voucher == null) {
            return;
        }
        if (voucher.getTotalDays() == null || voucher.getRemainingDays() == null) {
            throw new IllegalStateException("El bono tiene un número de días inválido");
        }
        voucher.setRemainingDays(Math.min(voucher.getTotalDays(), voucher.getRemainingDays() + 1));
        this.voucherRepository.save(voucher);
    }

    private ReservationEntity findReservation(Long reservationId) {
        Long validId = validateId(reservationId, "reserva");
        return this.reservationRepository.findById(validId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la reserva indicada"));
    }

    private Long validateId(Long id, String entityName) {
        if (Objects.isNull(id) || id <= 0) {
            throw new IllegalArgumentException("El identificador de " + entityName + " debe ser mayor que cero");
        }
        return id;
    }
}
