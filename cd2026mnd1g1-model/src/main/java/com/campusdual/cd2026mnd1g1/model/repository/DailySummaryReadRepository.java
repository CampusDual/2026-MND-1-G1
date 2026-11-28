package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DailySummaryReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a las marcas de lectura de resúmenes diarios por tutor.
 */
public interface DailySummaryReadRepository extends JpaRepository<DailySummaryReadEntity, Long> {

    Optional<DailySummaryReadEntity> findBySummary_IdAndTutor_Id(Long summaryId, Long tutorId);

    List<DailySummaryReadEntity> findByTutor_IdAndSummary_IdIn(Long tutorId, List<Long> summaryIds);
}
