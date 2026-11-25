package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroomingAppointmentRepository extends JpaRepository<GroomingAppointmentEntity, Long> {

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request", "request.customerCenter"
    })
    @Query(
            value = """
                    SELECT appointment FROM GroomingAppointmentEntity appointment
                    JOIN appointment.dog dog
                    JOIN appointment.groomer groomer
                    WHERE groomer.center.id = :centerId
                      AND dog.center.type = COALESCE(:originCenterType, dog.center.type)
                      AND appointment.status = COALESCE(:status, appointment.status)
                      AND (:fromDate IS NULL OR appointment.startAt >= :fromDate)
                      AND (:toDate IS NULL OR appointment.startAt < :toDate)
                      AND EXISTS (
                          SELECT service.id FROM GroomingServiceEntity service
                          WHERE service MEMBER OF appointment.services
                            AND service.center.id = :centerId
                      )
                      AND NOT EXISTS (
                          SELECT otherService.id FROM GroomingServiceEntity otherService
                          WHERE otherService MEMBER OF appointment.services
                            AND otherService.center.id <> :centerId
                      )
                    """,
            countQuery = """
                    SELECT COUNT(appointment.id) FROM GroomingAppointmentEntity appointment
                    JOIN appointment.dog dog
                    JOIN appointment.groomer groomer
                    WHERE groomer.center.id = :centerId
                      AND dog.center.type = COALESCE(:originCenterType, dog.center.type)
                      AND appointment.status = COALESCE(:status, appointment.status)
                      AND (:fromDate IS NULL OR appointment.startAt >= :fromDate)
                      AND (:toDate IS NULL OR appointment.startAt < :toDate)
                      AND EXISTS (
                          SELECT service.id FROM GroomingServiceEntity service
                          WHERE service MEMBER OF appointment.services
                            AND service.center.id = :centerId
                      )
                      AND NOT EXISTS (
                          SELECT otherService.id FROM GroomingServiceEntity otherService
                          WHERE otherService MEMBER OF appointment.services
                            AND otherService.center.id <> :centerId
                      )
                    """
    )
    Page<GroomingAppointmentEntity> findVisibleAppointments(
            @Param("centerId") Long centerId,
            @Param("originCenterType") CenterType originCenterType,
            @Param("status") GroomingRequestStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request", "request.customerCenter"
    })
    @Query("""
            SELECT appointment FROM GroomingAppointmentEntity appointment
            JOIN appointment.dog dog
            JOIN appointment.groomer groomer
            WHERE appointment.id = :appointmentId
              AND groomer.center.id = :centerId
              AND EXISTS (
                  SELECT service.id FROM GroomingServiceEntity service
                  WHERE service MEMBER OF appointment.services
                    AND service.center.id = :centerId
              )
              AND NOT EXISTS (
                  SELECT otherService.id FROM GroomingServiceEntity otherService
                  WHERE otherService MEMBER OF appointment.services
                    AND otherService.center.id <> :centerId
              )
            """)
    Optional<GroomingAppointmentEntity> findVisibleAppointment(
            @Param("appointmentId") Long appointmentId,
            @Param("centerId") Long centerId
    );

    @Query("""
            SELECT appointment.id AS appointmentId, service.name AS serviceName
            FROM GroomingAppointmentEntity appointment
            JOIN appointment.services service
            WHERE appointment.id IN :appointmentIds
              AND service.center.id = :centerId
            ORDER BY appointment.id, service.name
            """)
    List<GroomingAppointmentServiceNameProjection> findServiceNames(
            @Param("appointmentIds") List<Long> appointmentIds,
            @Param("centerId") Long centerId
    );
}
