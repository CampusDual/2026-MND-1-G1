package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DailyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecordEntity, Long> {
    List<DailyRecordEntity> findByReservation_IdOrderByOccurredAtDescIdDesc(Long reservationId);
}
