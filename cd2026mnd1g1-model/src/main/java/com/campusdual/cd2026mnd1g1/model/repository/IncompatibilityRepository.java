package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.IncompatibilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncompatibilityRepository extends JpaRepository<IncompatibilityEntity, Long> {

    List<IncompatibilityEntity> findByFirstDog_IdOrSecondDog_Id(Long firstDogId, Long secondDogId);

    @Query("""
            SELECT CASE WHEN COUNT(incompatibility) > 0 THEN TRUE ELSE FALSE END
            FROM IncompatibilityEntity incompatibility
            WHERE incompatibility.firstDog.id = :firstDogId
              AND incompatibility.secondDog.id = :secondDogId
            """)
    boolean existsBetweenDogs(
            @Param("firstDogId") Long firstDogId,
            @Param("secondDogId") Long secondDogId
    );
}
