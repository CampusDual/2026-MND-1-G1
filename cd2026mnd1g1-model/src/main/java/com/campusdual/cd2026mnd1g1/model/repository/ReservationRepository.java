package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.ReservationEntity;
import com.campusdual.cd2026mnd1g1.model.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    long countByGroup_IdAndDateAndStatusNot(Long groupId, LocalDate date, ReservationStatus status);

    List<ReservationEntity> findByDateAndGroup_IdAndStatusNot(
            LocalDate date,
            Long groupId,
            ReservationStatus status
    );

    List<ReservationEntity> findByDateAndDog_Center_Id(LocalDate date, Long centerId);

    List<ReservationEntity> findByDateAndDog_Center_IdAndGroup_Id(LocalDate date, Long centerId, Long groupId);
}
