package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.inventory.config.AppSecurityProperties;
import com.inventory.entity.AppUser;
import com.inventory.entity.Role;
import com.inventory.repository.AppUserRepository;
import com.inventory.security.AuthenticatedUser;
import com.inventory.security.InvalidTokenException;
import com.inventory.security.VerifiedToken;

class UserProvisioningServiceTest {

    private AppUserRepository repo;
    private UserProvisioningService service;

    @BeforeEach
    void setUp() {
        repo = mock(AppUserRepository.class);
        when(repo.saveAndFlush(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UserProvisioningService(repo, new AppSecurityProperties(List.of(" Admin@Example.com ")));
    }

    @Test
    void newUserGetsUserRole() {
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty());

        AuthenticatedUser user = service.resolve(new VerifiedToken("u1", "someone@example.com", true, "Sam"));

        assertThat(user.role()).isEqualTo(Role.USER);
        verify(repo).saveAndFlush(any(AppUser.class));
    }

    @Test
    void verifiedAllowlistedEmailGetsAdmin() {
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty());

        assertThat(service.resolve(new VerifiedToken("u1", "admin@example.com", true, null)).role())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void allowlistedButUnverifiedStaysUser() {
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty());

        assertThat(service.resolve(new VerifiedToken("u1", "admin@example.com", false, null)).role())
                .isEqualTo(Role.USER);
    }

    @Test
    void removedFromAllowlistIsDemoted() {
        AppUser stored = new AppUser("u1", "former@example.com", null, Role.ADMIN);
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.of(stored));

        assertThat(service.resolve(new VerifiedToken("u1", "former@example.com", true, null)).role())
                .isEqualTo(Role.USER);
        verify(repo, times(1)).saveAndFlush(stored);
    }

    @Test
    void noWriteWhenNothingChanged() {
        AppUser stored = new AppUser("u1", "someone@example.com", "Sam", Role.USER);
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.of(stored));

        service.resolve(new VerifiedToken("u1", "someone@example.com", true, "Sam"));

        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void duplicateKeyOnInsertRereadsExistingRow() {
        AppUser concurrent = new AppUser("u1", "someone@example.com", null, Role.USER);
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty(), Optional.of(concurrent));
        when(repo.saveAndFlush(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        AuthenticatedUser user = service.resolve(new VerifiedToken("u1", "someone@example.com", true, null));

        assertThat(user.firebaseUid()).isEqualTo("u1");
        assertThat(user.role()).isEqualTo(Role.USER);
    }

    @Test
    void displayNameIsSanitised() {
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty());
        when(repo.findByFirebaseUid("u2")).thenReturn(Optional.empty());

        assertThat(service.resolve(new VerifiedToken("u1", "a@example.com", true, "   ")).displayName()).isNull();
        assertThat(service.resolve(new VerifiedToken("u2", "b@example.com", true, "x".repeat(150))).displayName())
                .hasSize(100);
    }

    @Test
    void tokenWithoutEmailIsRejected() {
        assertThrows(InvalidTokenException.class,
                () -> service.resolve(new VerifiedToken("u1", null, false, null)));
        assertThrows(InvalidTokenException.class,
                () -> service.resolve(new VerifiedToken("u1", "  ", false, null)));
    }

    @Test
    void authoritiesMatchRole() {
        when(repo.findByFirebaseUid("u1")).thenReturn(Optional.empty());

        AuthenticatedUser admin = service.resolve(new VerifiedToken("u1", "admin@example.com", true, null));

        assertThat(admin.authorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }
}
