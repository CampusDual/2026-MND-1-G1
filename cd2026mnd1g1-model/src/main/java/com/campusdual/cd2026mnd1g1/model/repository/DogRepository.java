package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DogEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DogRepository extends JpaRepository<DogEntity, Long> {

    /**
     * Recupera una página de perros del tutor con los datos mínimos del portal.
     *
     * @param tutorId tutor vinculado a la cuenta
     * @param centerId centro de la cuenta autenticada
     * @param pageable orden y página
     * @return página del portal sin entidades internas
     */
    @Query("""
            SELECT dog FROM DogEntity dog
            WHERE dog.tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND dog.tutor.center.id = :centerId
            """)
    Page<DogEntity> findTutorPortalDogs(
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId,
            Pageable pageable
    );

    /**
     * Busca un perro solo si pertenece al tutor y al centro autenticados.
     *
     * @param dogId identificador solicitado
     * @param tutorId tutor vinculado a la cuenta
     * @param centerId centro de la cuenta autenticada
     * @return perro accesible si existe
     */
    @EntityGraph(attributePaths = "tutor")
    @Query("""
            SELECT dog FROM DogEntity dog
            JOIN dog.tutor tutor
            WHERE dog.id = :dogId
              AND tutor.id = :tutorId
              AND dog.center.id = :centerId
              AND tutor.center.id = :centerId
            """)
    Optional<DogEntity> findTutorPortalDog(
            @Param("dogId") Long dogId,
            @Param("tutorId") Long tutorId,
            @Param("centerId") Long centerId
    );

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT dog FROM DogEntity dog
            WHERE dog.id = :dogId
              AND dog.center.id = :centerId
            """)
    Optional<DogEntity> findByIdAndCenterIdForUpdate(
            @Param("dogId") Long dogId,
            @Param("centerId") Long centerId
    );

    @EntityGraph(attributePaths = "tutor")
    @Query("""
            SELECT dog FROM DogEntity dog
            JOIN dog.tutor tutor
            WHERE dog.center.id = :centerId
              AND tutor.center.id = :centerId
              AND dog.active = TRUE
            """)
    Page<DogEntity> findReservationCandidates(
            @Param("centerId") Long centerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "tutor")
    @Query("""
            SELECT dog FROM DogEntity dog
            JOIN dog.tutor tutor
            WHERE dog.center.id = :centerId
              AND tutor.center.id = :centerId
              AND dog.active = TRUE
              AND (
                   LOWER(dog.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(CONCAT(tutor.firstName, ' '), tutor.lastName))
                    LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<DogEntity> searchReservationCandidates(
            @Param("centerId") Long centerId,
            @Param("search") String search,
            Pageable pageable
    );

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
