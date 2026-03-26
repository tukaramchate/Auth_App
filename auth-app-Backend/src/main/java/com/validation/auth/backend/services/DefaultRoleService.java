package com.validation.auth.backend.services;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.validation.auth.backend.config.AppConstants;
import com.validation.auth.backend.entities.Role;
import com.validation.auth.backend.repositores.RoleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultRoleService {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void ensureSystemRolesExist() {
        ensureRole(AppConstants.ADMIN_ROLE);
        ensureRole(AppConstants.USER_ROLE);
        ensureRole(AppConstants.GUEST_ROLE);
    }

    public Set<Role> ensureAtLeastUserRole(Set<Role> roles) {
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        return new HashSet<>(Set.of(ensureRole(AppConstants.USER_ROLE)));
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }
}
