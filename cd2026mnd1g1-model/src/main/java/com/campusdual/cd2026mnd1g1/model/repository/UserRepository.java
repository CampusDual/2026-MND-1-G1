package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario
     * @return usuario encontrado si existe
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Recupera la cuenta autenticada y su vínculo explícito con tutor y centro.
     *
     * @param username nombre de acceso autenticado
     * @return cuenta encontrada si existe
     */
    @EntityGraph(attributePaths = {"tutor", "center", "tutor.center"})
    Optional<UserEntity> findPortalAccountByUsername(String username);

    /**
     * Busca una cuenta con el nombre de acceso o el email indicado.
     *
     * @param username nombre de acceso
     * @param email correo electrónico
     * @return usuario encontrado si existe
     */
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    /**
     * Comprueba si ya existe una cuenta con el email indicado.
     *
     * @param email correo electrónico
     * @return true si existe
     */
    boolean existsByEmail(String email);

    /**
     * Comprueba si existe un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario
     * @return true si existe
     */
    boolean existsByUsername(String username);

    /**
     * Lista cuentas activas con un rol concreto en un centro.
     *
     * @param centerId centro operativo
     * @param roleName nombre del rol
     * @return usuarios ordenados por nombre
     */
    @EntityGraph(attributePaths = {"center", "role"})
    List<UserEntity> findByCenter_IdAndRole_NameAndActiveTrueOrderByFirstNameAscLastNameAscIdAsc(
            Long centerId,
            String roleName
    );

    /**
     * Busca una cuenta activa con rol y centro concretos.
     *
     * @param id identificador de cuenta
     * @param centerId centro operativo
     * @param roleName nombre del rol
     * @return cuenta si pertenece al ámbito solicitado
     */
    @EntityGraph(attributePaths = {"center", "role"})
    Optional<UserEntity> findByIdAndCenter_IdAndRole_NameAndActiveTrue(Long id, Long centerId, String roleName);
}
