package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DogRepository extends JpaRepository<DogEntity, Long> {

    @EntityGraph(attributePaths = "tutor")
    @Query("""
            SELECT dog FROM DogEntity dog LEFT JOIN dog.tutor tutor
            WHERE dog.center.id = :centerId
              AND (tutor IS NULL OR tutor.center.id = dog.center.id)
            """)
    Page<DogEntity> findAllByCenter(
            @Param("centerId") Long centerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "tutor")
    @Query("""
            SELECT dog FROM DogEntity dog LEFT JOIN dog.tutor tutor
            WHERE dog.center.id = :centerId
              AND (tutor IS NULL OR tutor.center.id = dog.center.id)
              AND (
                   LOWER(dog.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(CONCAT(tutor.firstName, ' '), tutor.lastName))
                    LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<DogEntity> searchDogs(
            @Param("centerId") Long centerId,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "tutor")
    Optional<DogEntity> findByIdAndCenter_Id(Long id, Long centerId);

    @Query("""
            SELECT dog.id FROM DogEntity dog
            WHERE dog.center.id = :centerId
              AND dog.tutor.id = :tutorId
              AND LOWER(TRIM(dog.name)) = LOWER(:name)
              AND (:excludedDogId IS NULL OR dog.id <> :excludedDogId)
            """)
    List<Long> findPotentialDuplicateIds(
            @Param("centerId") Long centerId,
            @Param("tutorId") Long tutorId,
            @Param("name") String name,
            @Param("excludedDogId") Long excludedDogId
    );
}
