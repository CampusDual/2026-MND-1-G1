package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordEntity;
import com.campusdual.cd2026mnd1g1.model.entity.CheckRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CheckRecordRepository extends JpaRepository<CheckRecordEntity, Long> {

    /**
     * Recupera en una sola consulta los registros de presencia de varias reservas.
     *
     * @param reservationIds identificadores de las reservas
     * @return registros ordenados por reserva, fecha e identificador
     */
    List<CheckRecordEntity> findByReservation_IdInOrderByReservation_IdAscOccurredAtAscIdAsc(
            Collection<Long> reservationIds
    );

    boolean existsByReservation_IdAndType(Long reservationId, CheckRecordType type);

    java.util.Optional<CheckRecordEntity> findByReservation_IdAndType(
            Long reservationId,
            CheckRecordType type
    );
}
