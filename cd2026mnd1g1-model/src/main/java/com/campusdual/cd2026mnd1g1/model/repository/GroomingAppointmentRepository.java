package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingAppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface GroomingAppointmentRepository extends JpaRepository<GroomingAppointmentEntity, Long> {

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request",
            "request.customerCenter", "request.dog"
    })
    @Query(
            value = """
                    SELECT appointment FROM GroomingAppointmentEntity appointment
                    JOIN appointment.dog dog
                    LEFT JOIN appointment.groomer groomer
                    LEFT JOIN groomer.center groomerCenter
                    LEFT JOIN appointment.request request
                    WHERE (groomerCenter.id = :centerId
                           OR (groomer IS NULL AND dog.center.id = :centerId)
                           OR (groomer IS NULL
                               AND dog.center.type = :clientType
                               AND request.dog.id = dog.id
                               AND request.customerCenter.id = dog.center.id
                               AND EXISTS (
                                   SELECT requestedService.id FROM GroomingServiceEntity requestedService
                                   WHERE requestedService MEMBER OF request.services
                                     AND requestedService.center.id = :centerId
                               )
                               AND NOT EXISTS (
                                   SELECT otherRequestService.id FROM GroomingServiceEntity otherRequestService
                                   WHERE otherRequestService MEMBER OF request.services
                                     AND otherRequestService.center.id <> :centerId
                               )))
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
                    LEFT JOIN appointment.groomer groomer
                    LEFT JOIN groomer.center groomerCenter
                    LEFT JOIN appointment.request request
                    WHERE (groomerCenter.id = :centerId
                           OR (groomer IS NULL AND dog.center.id = :centerId)
                           OR (groomer IS NULL
                               AND dog.center.type = :clientType
                               AND request.dog.id = dog.id
                               AND request.customerCenter.id = dog.center.id
                               AND EXISTS (
                                   SELECT requestedService.id FROM GroomingServiceEntity requestedService
                                   WHERE requestedService MEMBER OF request.services
                                     AND requestedService.center.id = :centerId
                               )
                               AND NOT EXISTS (
                                   SELECT otherRequestService.id FROM GroomingServiceEntity otherRequestService
                                   WHERE otherRequestService MEMBER OF request.services
                                     AND otherRequestService.center.id <> :centerId
                               )))
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
            @Param("clientType") CenterType clientType,
            @Param("originCenterType") CenterType originCenterType,
            @Param("status") GroomingAppointmentStatus status,
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
            LEFT JOIN appointment.groomer groomer
            LEFT JOIN groomer.center groomerCenter
            LEFT JOIN appointment.request request
            WHERE appointment.id = :appointmentId
              AND (groomerCenter.id = :centerId
                   OR (groomer IS NULL AND dog.center.id = :centerId)
                   OR (groomer IS NULL
                       AND dog.center.type = :clientType
                       AND request.dog.id = dog.id
                       AND request.customerCenter.id = dog.center.id
                       AND EXISTS (
                           SELECT requestedService.id FROM GroomingServiceEntity requestedService
                           WHERE requestedService MEMBER OF request.services
                             AND requestedService.center.id = :centerId
                       )
                       AND NOT EXISTS (
                           SELECT otherRequestService.id FROM GroomingServiceEntity otherRequestService
                           WHERE otherRequestService MEMBER OF request.services
                             AND otherRequestService.center.id <> :centerId
                       )))
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
            @Param("centerId") Long centerId,
            @Param("clientType") CenterType clientType
    );

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request",
            "request.customerCenter", "request.dog"
    })
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT appointment FROM GroomingAppointmentEntity appointment
            JOIN appointment.dog dog
            LEFT JOIN appointment.groomer groomer
            LEFT JOIN groomer.center groomerCenter
            LEFT JOIN appointment.request request
            WHERE appointment.id = :appointmentId
              AND (groomerCenter.id = :centerId
                   OR (groomer IS NULL AND dog.center.id = :centerId)
                   OR (groomer IS NULL
                       AND dog.center.type = :clientType
                       AND request.dog.id = dog.id
                       AND request.customerCenter.id = dog.center.id
                       AND EXISTS (
                           SELECT requestedService.id FROM GroomingServiceEntity requestedService
                           WHERE requestedService MEMBER OF request.services
                             AND requestedService.center.id = :centerId
                       )
                       AND NOT EXISTS (
                           SELECT otherRequestService.id FROM GroomingServiceEntity otherRequestService
                           WHERE otherRequestService MEMBER OF request.services
                             AND otherRequestService.center.id <> :centerId
                       )))
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
    Optional<GroomingAppointmentEntity> findVisibleAppointmentForUpdate(
            @Param("appointmentId") Long appointmentId,
            @Param("centerId") Long centerId,
            @Param("clientType") CenterType clientType
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

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request", "request.customerCenter"
    })
    @Query("""
            SELECT appointment FROM GroomingAppointmentEntity appointment
            JOIN appointment.groomer groomer
            JOIN appointment.dog dog
            WHERE groomer.id = :groomerId
              AND groomer.center.id = :centerId
              AND dog.center.type IN :originTypes
              AND appointment.startAt >= :fromDate
              AND appointment.startAt < :toDate
              AND appointment.status <> :unassignedStatus
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
            ORDER BY appointment.startAt, appointment.id
            """)
    List<GroomingAppointmentEntity> findGroomerAgenda(
            @Param("groomerId") Long groomerId,
            @Param("centerId") Long centerId,
            @Param("originTypes") Collection<CenterType> originTypes,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("unassignedStatus") GroomingAppointmentStatus unassignedStatus
    );

    @EntityGraph(attributePaths = {
            "dog", "dog.tutor", "dog.center", "groomer", "groomer.center", "request", "request.customerCenter"
    })
    @Query("""
            SELECT appointment FROM GroomingAppointmentEntity appointment
            JOIN appointment.groomer groomer
            JOIN appointment.dog dog
            WHERE appointment.id = :appointmentId
              AND groomer.id = :groomerId
              AND groomer.center.id = :centerId
              AND appointment.status <> :unassignedStatus
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
    Optional<GroomingAppointmentEntity> findGroomerAgendaAppointment(
            @Param("appointmentId") Long appointmentId,
            @Param("groomerId") Long groomerId,
            @Param("centerId") Long centerId,
            @Param("unassignedStatus") GroomingAppointmentStatus unassignedStatus
    );
}
