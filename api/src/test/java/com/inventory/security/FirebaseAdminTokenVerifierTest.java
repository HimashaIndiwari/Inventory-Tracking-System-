package com.inventory.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

class FirebaseAdminTokenVerifierTest {

    private FirebaseAuth auth;
    private FirebaseAdminTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        auth = mock(FirebaseAuth.class);
        verifier = new FirebaseAdminTokenVerifier(auth);
    }

    private void failWith(com.google.firebase.ErrorCode platform, AuthErrorCode authCode, Throwable cause)
            throws FirebaseAuthException {
        when(auth.verifyIdToken(anyString(), eq(true)))
                .thenThrow(new FirebaseAuthException(platform, "failed", cause, null, authCode));
    }

    @Test
    void callsVerifyIdTokenWithRevocationCheck() throws Exception {
        when(auth.verifyIdToken("t", true)).thenReturn(mock(FirebaseToken.class));

        verifier.verify("t");

        verify(auth).verifyIdToken("t", true);
        verify(auth, never()).verifyIdToken("t");
    }

    @Test
    void mapsDecodedTokenFields() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(token.getEmail()).thenReturn("a@example.com");
        when(token.isEmailVerified()).thenReturn(true);
        when(token.getName()).thenReturn("Ann");
        when(auth.verifyIdToken("t", true)).thenReturn(token);

        assertThat(verifier.verify("t")).isEqualTo(new VerifiedToken("uid-1", "a@example.com", true, "Ann"));
    }

    @ParameterizedTest
    @EnumSource(value = AuthErrorCode.class,
            names = {"INVALID_ID_TOKEN", "EXPIRED_ID_TOKEN", "REVOKED_ID_TOKEN", "USER_DISABLED", "USER_NOT_FOUND"})
    void authFailuresAreInvalidToken(AuthErrorCode code) throws Exception {
        failWith(com.google.firebase.ErrorCode.INVALID_ARGUMENT, code, null);
        assertThrows(InvalidTokenException.class, () -> verifier.verify("t"));
    }

    @Test
    void certificateFetchFailureIsUnavailable() throws Exception {
        failWith(com.google.firebase.ErrorCode.UNKNOWN, AuthErrorCode.CERTIFICATE_FETCH_FAILED, null);
        assertThrows(AuthServiceUnavailableException.class, () -> verifier.verify("t"));
    }

    @ParameterizedTest
    @EnumSource(value = com.google.firebase.ErrorCode.class,
            names = {"UNAVAILABLE", "DEADLINE_EXCEEDED", "RESOURCE_EXHAUSTED"})
    void platformOutagesAreUnavailable(com.google.firebase.ErrorCode code) throws Exception {
        failWith(code, null, null);
        assertThrows(AuthServiceUnavailableException.class, () -> verifier.verify("t"));
    }

    @Test
    void ioFailureIsUnavailable() throws Exception {
        failWith(com.google.firebase.ErrorCode.UNKNOWN, null, new IOException("connection reset"));
        assertThrows(AuthServiceUnavailableException.class, () -> verifier.verify("t"));
    }

    @Test
    void otherErrorsAreVerificationFailures() throws Exception {
        failWith(com.google.firebase.ErrorCode.PERMISSION_DENIED, null, null);
        assertThrows(AuthVerificationException.class, () -> verifier.verify("t"));
    }

    @Test
    void otherAuthCodesAreVerificationFailures() throws Exception {
        failWith(com.google.firebase.ErrorCode.NOT_FOUND, AuthErrorCode.TENANT_NOT_FOUND, null);
        assertThrows(AuthVerificationException.class, () -> verifier.verify("t"));
    }
}
