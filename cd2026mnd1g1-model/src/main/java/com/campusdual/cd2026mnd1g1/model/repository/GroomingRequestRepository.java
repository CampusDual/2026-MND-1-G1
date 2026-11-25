package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.CenterType;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestEntity;
import com.campusdual.cd2026mnd1g1.model.entity.GroomingRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroomingRequestRepository extends JpaRepository<GroomingRequestEntity, Long> {

    @EntityGraph(attributePaths = {"dog", "dog.tutor", "dog.center", "customerCenter"})
    @Query(
            value = """
                    SELECT DISTINCT request FROM GroomingRequestEntity request
                    JOIN request.dog dog
                    JOIN request.services service
                    WHERE service.center.id = :serviceCenterId
                      AND request.customerCenter.id = dog.center.id
                      AND dog.center.type = COALESCE(:originCenterType, dog.center.type)
                      AND request.status = COALESCE(:status, request.status)
                      AND request.requestedDate >= COALESCE(:fromDate, request.requestedDate)
                      AND request.requestedDate <= COALESCE(:toDate, request.requestedDate)
                      AND NOT EXISTS (
                          SELECT otherService.id FROM GroomingServiceEntity otherService
                          WHERE otherService MEMBER OF request.services
                            AND otherService.center.id <> :serviceCenterId
                      )
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT request.id) FROM GroomingRequestEntity request
                    JOIN request.dog dog
                    JOIN request.services service
                    WHERE service.center.id = :serviceCenterId
                      AND request.customerCenter.id = dog.center.id
                      AND dog.center.type = COALESCE(:originCenterType, dog.center.type)
                      AND request.status = COALESCE(:status, request.status)
                      AND request.requestedDate >= COALESCE(:fromDate, request.requestedDate)
                      AND request.requestedDate <= COALESCE(:toDate, request.requestedDate)
                      AND NOT EXISTS (
                          SELECT otherService.id FROM GroomingServiceEntity otherService
                          WHERE otherService MEMBER OF request.services
                            AND otherService.center.id <> :serviceCenterId
                      )
                    """
    )
    Page<GroomingRequestEntity> findVisibleRequests(
            @Param("serviceCenterId") Long serviceCenterId,
            @Param("originCenterType") CenterType originCenterType,
            @Param("status") GroomingRequestStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"dog", "dog.tutor", "dog.center", "customerCenter"})
    @Query("""
            SELECT DISTINCT request FROM GroomingRequestEntity request
            JOIN request.dog dog
            JOIN request.services service
            WHERE request.id = :requestId
              AND service.center.id = :serviceCenterId
              AND request.customerCenter.id = dog.center.id
              AND NOT EXISTS (
                  SELECT otherService.id FROM GroomingServiceEntity otherService
                  WHERE otherService MEMBER OF request.services
                    AND otherService.center.id <> :serviceCenterId
              )
            """)
    Optional<GroomingRequestEntity> findVisibleRequest(
            @Param("requestId") Long requestId,
            @Param("serviceCenterId") Long serviceCenterId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT DISTINCT request FROM GroomingRequestEntity request
            JOIN request.dog dog
            JOIN request.services service
            WHERE request.id = :requestId
              AND service.center.id = :serviceCenterId
              AND request.customerCenter.id = dog.center.id
              AND NOT EXISTS (
                  SELECT otherService.id FROM GroomingServiceEntity otherService
                  WHERE otherService MEMBER OF request.services
                    AND otherService.center.id <> :serviceCenterId
              )
            """)
    Optional<GroomingRequestEntity> findVisibleRequestForUpdate(
            @Param("requestId") Long requestId,
            @Param("serviceCenterId") Long serviceCenterId
    );

    @Query("""
            SELECT request.id AS requestId, service.name AS serviceName
            FROM GroomingRequestEntity request
            JOIN request.services service
            WHERE request.id IN :requestIds
              AND service.center.id = :serviceCenterId
            ORDER BY request.id, service.name
            """)
    List<GroomingRequestServiceNameProjection> findServiceNames(
            @Param("requestIds") List<Long> requestIds,
            @Param("serviceCenterId") Long serviceCenterId
    );
}
