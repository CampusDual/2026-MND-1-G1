package com.campusdual.cd2026mnd1g1.model.service;

import com.campusdual.cd2026mnd1g1.api.dto.UserDto;
import com.campusdual.cd2026mnd1g1.api.dto.UserUpsertRequestDto;
import com.campusdual.cd2026mnd1g1.api.service.UserService;
import com.campusdual.cd2026mnd1g1.model.entity.CenterEntity;
import com.campusdual.cd2026mnd1g1.model.entity.RoleEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.exception.ResourceNotFoundException;
import com.campusdual.cd2026mnd1g1.model.exception.UserNotFoundException;
import com.campusdual.cd2026mnd1g1.model.repository.CenterRepository;
import com.campusdual.cd2026mnd1g1.model.repository.RoleRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CenterRepository centerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CenterRepository centerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.centerRepository = centerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return this.userRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        validateId(id);
        UserEntity userEntity = this.userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return mapToDto(userEntity);
    }

    @Override
    public UserDto create(UserUpsertRequestDto userRequest) {
        validateRequest(userRequest);
        String username = normalizeUsername(userRequest.getUsername());
        String email = normalizeUsername(userRequest.getEmail());
        if (this.userRepository.existsByUsername(username) || this.userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre o email");
        }

        UserEntity userEntity = new UserEntity();
        applyRequest(userEntity, userRequest, username, email);
        return mapToDto(this.userRepository.save(userEntity));
    }

    @Override
    public UserDto update(Long id, UserUpsertRequestDto userRequest) {
        validateId(id);
        validateRequest(userRequest);
        UserEntity userEntity = this.userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String username = normalizeUsername(userRequest.getUsername());
        String email = normalizeUsername(userRequest.getEmail());
        this.userRepository.findByUsernameOrEmail(username, username)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese nombre");
                });
        this.userRepository.findByUsernameOrEmail(email, email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un usuario con ese email");
                });

        applyRequest(userEntity, userRequest, username, email);
        return mapToDto(this.userRepository.save(userEntity));
    }

    @Override
    public void delete(Long id) {
        validateId(id);
        if (!this.userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        this.userRepository.deleteById(id);
    }

    private void applyRequest(UserEntity userEntity, UserUpsertRequestDto request, String username, String email) {
        CenterEntity center = this.centerRepository.findById(request.getCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el centro indicado"));
        userEntity.setCenter(center);
        userEntity.setRole(resolveRole(request.getRoles().iterator().next()));
        userEntity.setFirstName(request.getFirstName().trim());
        userEntity.setLastName(request.getLastName().trim());
        userEntity.setNationalId(request.getNationalId().trim().toUpperCase(Locale.ROOT));
        userEntity.setEmail(email);
        userEntity.setUsername(username);
        userEntity.setPasswordHash(this.passwordEncoder.encode(request.getPassword()));
        userEntity.setActive(request.getEnabled());
        if (userEntity.getCreatedAt() == null) {
            userEntity.setCreatedAt(LocalDateTime.now());
        }
    }

    private UserDto mapToDto(UserEntity userEntity) {
        Set<String> roles = new LinkedHashSet<>();
        String roleName = userEntity.getRole().getName();
        roles.add(roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName);
        return new UserDto(userEntity.getId(), userEntity.getUsername(), userEntity.getActive(), roles);
    }

    private RoleEntity resolveRole(String roleName) {
        String normalizedRole = normalizeRoleName(roleName);
        return this.roleRepository.findByName(normalizedRole).orElseGet(() -> createRole(normalizedRole));
    }

    private RoleEntity createRole(String roleName) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(roleName);
        roleEntity.setDescription("Rol creado desde la gestión de usuarios");
        return this.roleRepository.save(roleEntity);
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El id debe ser mayor que cero");
        }
    }

    private void validateRequest(UserUpsertRequestDto userRequest) {
        if (Objects.isNull(userRequest)) {
            throw new IllegalArgumentException("El cuerpo de la petición no puede ser nulo");
        }
        if (userRequest.getCenterId() == null || userRequest.getCenterId() <= 0) {
            throw new IllegalArgumentException("El centro es obligatorio y debe ser mayor que cero");
        }
        requireText(userRequest.getFirstName(), "El nombre es obligatorio");
        requireText(userRequest.getLastName(), "Los apellidos son obligatorios");
        requireText(userRequest.getNationalId(), "El DNI es obligatorio");
        requireText(userRequest.getEmail(), "El email es obligatorio");
        requireText(userRequest.getPassword(), "La contraseña es obligatoria");
        requireText(userRequest.getUsername(), "El nombre de usuario es obligatorio");
        if (userRequest.getRoles() == null || userRequest.getRoles().size() != 1) {
            throw new IllegalArgumentException("Debe indicarse exactamente un rol");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario o email es obligatorio");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol no puede estar vacío");
        }
        String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        return normalizedRole;
    }
}
