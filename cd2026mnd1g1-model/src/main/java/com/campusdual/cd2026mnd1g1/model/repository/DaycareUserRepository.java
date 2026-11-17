package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.DaycareUserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DaycareUserRepository extends JpaRepository<DaycareUserEntity, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<DaycareUserEntity> findByEmail(String email);
}
