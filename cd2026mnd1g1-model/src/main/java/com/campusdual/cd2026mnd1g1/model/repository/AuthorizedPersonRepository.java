package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.AuthorizedPersonEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizedPersonRepository extends JpaRepository<AuthorizedPersonEntity, Long> {

    /**
     * Lista las personas autorizadas de un tutor para mostrarlas en recepción.
     *
     * @param tutorId tutor titular de las autorizaciones
     * @return personas ordenadas por nombre e identificador
     */
    List<AuthorizedPersonEntity> findByTutor_IdOrderByNameAscIdAsc(Long tutorId);

    /**
     * Revalida y bloquea una autorización dentro de la transacción de salida.
     *
     * @param personId identificador de la persona autorizada
     * @param tutorId tutor asociado a la reserva
     * @return autorización si pertenece al tutor indicado
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT person FROM AuthorizedPersonEntity person
            WHERE person.id = :personId
              AND person.tutor.id = :tutorId
            """)
    Optional<AuthorizedPersonEntity> findByIdAndTutorIdForUpdate(
            @Param("personId") Long personId,
            @Param("tutorId") Long tutorId
    );
}
