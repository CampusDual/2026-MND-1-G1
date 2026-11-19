package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.MedicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<MedicationEntity, Long> {

    List<MedicationEntity> findByDog_IdOrderByIdAsc(Long dogId);

    List<MedicationEntity> findByDog_Id(Long dogId);
}
