package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.TutorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface TutorRepository extends JpaRepository<TutorEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tutor FROM TutorEntity tutor JOIN FETCH tutor.center WHERE tutor.id = :id")
    Optional<TutorEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<TutorEntity> findByIdAndCenter_Id(Long id, Long centerId);

    Page<TutorEntity> findByCenter_Id(Long centerId, Pageable pageable);

    boolean existsByCenter_IdAndNationalIdIgnoreCase(Long centerId, String nationalId);

    Optional<TutorEntity> findByCenter_IdAndNationalIdIgnoreCase(Long centerId, String nationalId);

    @EntityGraph(attributePaths = {"center"})
    @Query("""
            SELECT tutor FROM TutorEntity tutor
            WHERE tutor.center.id = :centerId
              AND (
                   LOWER(tutor.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(CONCAT(CONCAT(tutor.firstName, ' '), tutor.lastName))
                    LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.nationalId) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(tutor.email) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<TutorEntity> searchTutors(
            @Param("centerId") Long centerId,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"center"})
    @Query("""
            SELECT tutor FROM TutorEntity tutor
            WHERE tutor.center.id = :centerId
              AND (
                   LOWER(TRIM(tutor.phone)) = LOWER(:phone)
                OR LOWER(tutor.email) = LOWER(:email)
              )
            """)
    List<TutorEntity> findPotentialMatches(
            @Param("centerId") Long centerId,
            @Param("phone") String phone,
            @Param("email") String email
    );
}
