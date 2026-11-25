package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DailySummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
