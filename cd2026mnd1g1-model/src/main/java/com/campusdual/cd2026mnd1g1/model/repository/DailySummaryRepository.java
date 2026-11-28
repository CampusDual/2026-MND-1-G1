package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DailySummaryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso al resumen publicado por reserva.
 */
public interface DailySummaryRepository extends JpaRepository<DailySummaryEntity, Long> {

    Optional<DailySummaryEntity> findByReservation_IdAndRecipientAndStatus(
            Long reservationId,
            String recipient,
            String status
    );

    @Query("""
            SELECT summary FROM DailySummaryEntity summary
            JOIN FETCH summary.reservation reservation
            JOIN FETCH reservation.dog dog
            WHERE dog.id = :dogId
              AND dog.tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND dog.tutor.center.id = :centerId
              AND summary.recipient = 'TUTOR'
              AND summary.status = 'PUBLICADO'
            ORDER BY reservation.date DESC, summary.id DESC
            """)
    List<DailySummaryEntity> findPublishedTutorSummaries(
            @Param("dogId") Long dogId,
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId
    );

    @Query("""
            SELECT summary FROM DailySummaryEntity summary
            JOIN FETCH summary.reservation reservation
            JOIN FETCH reservation.dog dog
            WHERE summary.id = :summaryId
              AND dog.id = :dogId
              AND dog.tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND dog.tutor.center.id = :centerId
              AND summary.recipient = 'TUTOR'
              AND summary.status = 'PUBLICADO'
            """)
    Optional<DailySummaryEntity> findPublishedTutorSummary(
            @Param("summaryId") Long summaryId,
            @Param("dogId") Long dogId,
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT summary FROM DailySummaryEntity summary
            JOIN FETCH summary.reservation reservation
            JOIN FETCH reservation.dog dog
            WHERE summary.id = :summaryId
              AND dog.id = :dogId
              AND dog.tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND dog.tutor.center.id = :centerId
              AND summary.recipient = 'TUTOR'
              AND summary.status = 'PUBLICADO'
            """)
    Optional<DailySummaryEntity> findPublishedTutorSummaryForUpdate(
            @Param("summaryId") Long summaryId,
            @Param("dogId") Long dogId,
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId
    );

    @Query("""
            SELECT COUNT(summary) FROM DailySummaryEntity summary
            JOIN summary.reservation reservation
            JOIN reservation.dog dog
            WHERE dog.tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND dog.tutor.center.id = :centerId
              AND summary.recipient = 'TUTOR'
              AND summary.status = 'PUBLICADO'
              AND NOT EXISTS (
                  SELECT readRecord.id FROM DailySummaryReadEntity readRecord
                  WHERE readRecord.summary = summary
                    AND readRecord.tutor.id = :tutorId
              )
            """)
    long countUnreadTutorSummaries(
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId
    );
}
