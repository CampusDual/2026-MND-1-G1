package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.TutorDogSubmissionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TutorDogSubmissionRepository extends JpaRepository<TutorDogSubmissionEntity, Long> {

    @EntityGraph(attributePaths = {"dog", "dog.tutor", "dog.center"})
    Optional<TutorDogSubmissionEntity> findByTutor_IdAndIdempotencyKey(Long tutorId, String idempotencyKey);
}
