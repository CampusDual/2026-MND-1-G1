package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareGroupEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DaycareGroupRepository extends JpaRepository<DaycareGroupEntity, Long> {

    List<DaycareGroupEntity> findByCenter_IdOrderByNameAsc(Long centerId);

    Optional<DaycareGroupEntity> findByIdAndCenter_Id(Long id, Long centerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT daycareGroup FROM DaycareGroupEntity daycareGroup
            WHERE daycareGroup.id = :groupId
              AND daycareGroup.center.id = :centerId
            """)
    Optional<DaycareGroupEntity> findByIdAndCenterIdForUpdate(
            @Param("groupId") Long groupId,
            @Param("centerId") Long centerId
    );
}
