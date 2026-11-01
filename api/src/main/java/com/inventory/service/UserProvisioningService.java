package com.inventory.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.inventory.config.AppSecurityProperties;
import com.inventory.entity.AppUser;
import com.inventory.entity.Role;
import com.inventory.repository.AppUserRepository;
import com.inventory.security.AuthenticatedUser;
import com.inventory.security.InvalidTokenException;
import com.inventory.security.VerifiedToken;

/**
 * Finds or creates the MySQL user for a verified Firebase token and resolves the role on every request
 * (spec §4.3): ADMIN only when the email is verified AND on the allowlist, otherwise USER. Writes only when
 * something changed. Not @Transactional: a duplicate-key insert from a concurrent first request is recovered by
 * re-reading the existing row.
 */
@Service
public class UserProvisioningService {

    static final int DISPLAY_NAME_MAX = 100;

    private final AppUserRepository users;
    private final Set<String> adminEmails;

    public UserProvisioningService(AppUserRepository users, AppSecurityProperties securityProperties) {
        this.users = users;
        this.adminEmails = securityProperties.adminEmails().stream()
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public AuthenticatedUser resolve(VerifiedToken token) {
        String email = token.email() == null ? "" : token.email().trim();
        if (email.isEmpty()) {
            throw new InvalidTokenException("Token has no email address.");
        }
        Role role = token.emailVerified() && adminEmails.contains(email.toLowerCase(Locale.ROOT))
                ? Role.ADMIN : Role.USER;
        String displayName = sanitiseDisplayName(token.name());

        AppUser user = users.findByFirebaseUid(token.uid()).orElse(null);
        if (user == null) {
            try {
                user = users.saveAndFlush(new AppUser(token.uid(), email, displayName, role));
            } catch (DataIntegrityViolationException duplicate) {
                user = users.findByFirebaseUid(token.uid()).orElseThrow(() -> duplicate);
            }
        }
        if (!Objects.equals(user.getEmail(), email) || !Objects.equals(user.getDisplayName(), displayName)
                || user.getRole() != role) {
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setRole(role);
            user = users.saveAndFlush(user);
        }
        return new AuthenticatedUser(user.getId(), user.getFirebaseUid(), user.getEmail(), user.getDisplayName(),
                user.getRole());
    }

    /** The token's name claim is untrusted: trim, blank → null, truncate to the column size. */
    static String sanitiseDisplayName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > DISPLAY_NAME_MAX ? trimmed.substring(0, DISPLAY_NAME_MAX) : trimmed;
    }
}
