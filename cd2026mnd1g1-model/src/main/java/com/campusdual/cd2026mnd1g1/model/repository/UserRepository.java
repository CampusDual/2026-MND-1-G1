package com.campusdual.cd2026mnd1g1.model.repository;

import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
}
