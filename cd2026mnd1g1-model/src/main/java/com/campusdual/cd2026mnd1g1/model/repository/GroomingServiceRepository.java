package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.GroomingServiceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroomingServiceRepository extends JpaRepository<GroomingServiceEntity, Long> {

    List<GroomingServiceEntity> findByCenter_IdAndActiveTrueOrderByNameAscIdAsc(Long centerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroomingServiceEntity> findByIdAndCenter_IdAndActiveTrue(Long id, Long centerId);
}
