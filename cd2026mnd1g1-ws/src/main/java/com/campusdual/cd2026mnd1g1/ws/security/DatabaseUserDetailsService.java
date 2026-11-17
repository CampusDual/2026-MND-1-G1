package com.campusdual.cd2026mnd1g1.ws.security;

import com.campusdual.cd2026mnd1g1.model.entity.UserEntity;
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

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DatabaseUserDetailsService(
            UserRepository userRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Carga un usuario de seguridad por nombre de acceso o correo electrónico.
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

        UserEntity userEntity = this.userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario solicitado"));
        return buildUserDetails(userEntity);
    }

    private UserDetails buildUserDetails(UserEntity userEntity) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        String roleName = userEntity.getRole().getName();
        String roleAuthority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        authorities.add(new SimpleGrantedAuthority(roleAuthority));

        List<String> permissionCodes = this.rolePermissionRepository
                .findPermissionCodesByRoleId(userEntity.getRole().getId());
        permissionCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return User.withUsername(userEntity.getUsername())
                .password(userEntity.getPasswordHash())
                .authorities(authorities)
                .disabled(!Boolean.TRUE.equals(userEntity.getActive()))
                .build();
    }
}
