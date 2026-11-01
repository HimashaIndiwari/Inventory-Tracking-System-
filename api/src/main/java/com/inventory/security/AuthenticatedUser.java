package com.inventory.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.inventory.entity.Role;

/** The resolved user for the current request (principal in the SecurityContext). */
public record AuthenticatedUser(Long userId, String firebaseUid, String email, String displayName, Role role) {

    public Collection<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
