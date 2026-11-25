package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationModality;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    long countByGroup_IdAndDateAndStatusNot(Long groupId, LocalDate date, ReservationStatus status);

    long countByGroup_IdAndDateAndStatusNotAndModalityIn(
            Long groupId,
            LocalDate date,
            ReservationStatus status,
            List<ReservationModality> modalities
    );

    long countByGroup_IdAndDateAndStatusNotAndModalityInAndIdNot(
            Long groupId,
            LocalDate date,
            ReservationStatus status,
            List<ReservationModality> modalities,
            Long excludedReservationId
    );

    boolean existsByDog_IdAndDateAndStatusNot(Long dogId, LocalDate date, ReservationStatus status);

    List<ReservationEntity> findByDateAndGroup_IdAndStatusNot(
            LocalDate date,
            Long groupId,
            ReservationStatus status
    );

    List<ReservationEntity> findByDateAndDog_Center_Id(LocalDate date, Long centerId);

    @EntityGraph(attributePaths = {"dog", "group"})
    @Query("""
            SELECT reservation FROM ReservationEntity reservation
            JOIN reservation.group group
            JOIN group.caregiver caregiver
            WHERE caregiver.id = :caregiverId
              AND caregiver.center.id = :centerId
              AND reservation.dog.center.id = :centerId
              AND reservation.date = :today
              AND reservation.status = com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus.EN_CURSO
              AND EXISTS (
                  SELECT entry.id FROM CheckRecordEntity entry
                  WHERE entry.reservation.id = reservation.id
                    AND entry.type = com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType.ENTRADA
              )
              AND NOT EXISTS (
                  SELECT exit.id FROM CheckRecordEntity exit
                  WHERE exit.reservation.id = reservation.id
                    AND exit.type = com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType.SALIDA
              )
            """)
    Page<ReservationEntity> findCaregiverTodayReservations(
            @Param("caregiverId") Long caregiverId,
            @Param("centerId") Long centerId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"dog", "dog.tutor", "group"})
    @Query("""
            SELECT reservation FROM ReservationEntity reservation
            JOIN reservation.group group
            JOIN group.caregiver caregiver
            WHERE reservation.id = :reservationId
              AND caregiver.id = :caregiverId
              AND caregiver.center.id = :centerId
              AND reservation.dog.center.id = :centerId
              AND reservation.status = com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus.EN_CURSO
              AND reservation.date = :today
              AND EXISTS (
                  SELECT entry.id FROM CheckRecordEntity entry
                  WHERE entry.reservation.id = reservation.id
                    AND entry.type = com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType.ENTRADA
              )
              AND NOT EXISTS (
                  SELECT exit.id FROM CheckRecordEntity exit
                  WHERE exit.reservation.id = reservation.id
                    AND exit.type = com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType.SALIDA
              )
            """)
    Optional<ReservationEntity> findCaregiverTodayReservation(
            @Param("reservationId") Long reservationId,
            @Param("caregiverId") Long caregiverId,
            @Param("centerId") Long centerId,
            @Param("today") LocalDate today
    );

    List<ReservationEntity> findByDateAndDog_Center_IdAndGroup_Id(LocalDate date, Long centerId, Long groupId);

    @EntityGraph(attributePaths = {"dog", "dog.tutor"})
    Page<ReservationEntity> findByDateAndDog_Center_Id(
            LocalDate date,
            Long centerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"dog", "dog.tutor"})
    Optional<ReservationEntity> findByIdAndDog_Center_Id(Long id, Long centerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT reservation FROM ReservationEntity reservation
            WHERE reservation.id = :reservationId
              AND reservation.dog.center.id = :centerId
            """)
    Optional<ReservationEntity> findByIdAndCenterIdForUpdate(
            @Param("reservationId") Long reservationId,
            @Param("centerId") Long centerId
    );
}
