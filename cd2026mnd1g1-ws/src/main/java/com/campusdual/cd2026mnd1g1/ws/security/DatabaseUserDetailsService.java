package com.campusdual.cd2026mnd1g1.ws.security;

import com.campusdual.cd2026mnd1g1.model.entity.RoleEntity;
import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
import com.campusdual.cd2026mnd1g1.model.entity.DaycareUserEntity;
import com.campusdual.cd2026mnd1g1.model.repository.DaycareUserRepository;
import com.campusdual.cd2026mnd1g1.model.repository.RolePermissionRepository;
import com.campusdual.cd2026mnd1g1.model.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DaycareUserRepository daycareUserRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DatabaseUserDetailsService(
            UserRepository userRepository,
            DaycareUserRepository daycareUserRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userRepository = userRepository;
        this.daycareUserRepository = daycareUserRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Carga un usuario de seguridad a partir del nombre de usuario de base de datos.
     *
     * @param username nombre de usuario
     * @return usuario con authorities para Spring Security
     * @throws UsernameNotFoundException si no existe el usuario
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("No existe el usuario solicitado");
        }

        DaycareUserEntity daycareUser = this.daycareUserRepository.findByEmail(username).orElse(null);
        if (daycareUser != null) {
            return buildDaycareUserDetails(daycareUser);
        }

        UserEntity userEntity = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario solicitado"));
        Set<GrantedAuthority> authorities = userEntity.getRoles().stream()
                .map(RoleEntity::getName)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return User.withUsername(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(authorities)
                .disabled(!Boolean.TRUE.equals(userEntity.getEnabled()))
                .build();
    }

    private UserDetails buildDaycareUserDetails(DaycareUserEntity userEntity) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        String roleName = userEntity.getRole().getName();
        String roleAuthority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        authorities.add(new SimpleGrantedAuthority(roleAuthority));

        List<String> permissionCodes = this.rolePermissionRepository
                .findPermissionCodesByRoleId(userEntity.getRole().getId());
        permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return User.withUsername(userEntity.getEmail())
                .password(userEntity.getPasswordHash())
                .authorities(authorities)
                .disabled(!Boolean.TRUE.equals(userEntity.getActive()))
                .build();
    }
}

