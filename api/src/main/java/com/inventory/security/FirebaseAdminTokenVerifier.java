package com.inventory.security;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

/**
 * Production verifier: {@code verifyIdToken(token, true)} enables revocation checking. Firebase's platform
 * enum {@code com.google.firebase.ErrorCode} is referenced by its full name because the project has its own
 * {@code ErrorCode}. Only error codes are logged, never the token.
 */
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminTokenVerifier.class);

    private static final Set<AuthErrorCode> INVALID = EnumSet.of(AuthErrorCode.INVALID_ID_TOKEN,
            AuthErrorCode.EXPIRED_ID_TOKEN, AuthErrorCode.REVOKED_ID_TOKEN, AuthErrorCode.USER_DISABLED,
            AuthErrorCode.USER_NOT_FOUND);

    private static final Set<com.google.firebase.ErrorCode> OUTAGE = EnumSet.of(
            com.google.firebase.ErrorCode.UNAVAILABLE, com.google.firebase.ErrorCode.DEADLINE_EXCEEDED,
            com.google.firebase.ErrorCode.RESOURCE_EXHAUSTED);

    private final FirebaseAuth auth;

    public FirebaseAdminTokenVerifier(FirebaseAuth auth) {
        this.auth = auth;
    }

    @Override
    public VerifiedToken verify(String idToken) {
        try {
            FirebaseToken token = auth.verifyIdToken(idToken, true);
            return new VerifiedToken(token.getUid(), token.getEmail(), token.isEmailVerified(), token.getName());
        } catch (FirebaseAuthException e) {
            throw classify(e);
        }
    }

    private RuntimeException classify(FirebaseAuthException e) {
        AuthErrorCode authCode = e.getAuthErrorCode();
        com.google.firebase.ErrorCode platformCode = e.getErrorCode();
        if (authCode != null && INVALID.contains(authCode)) {
            return new InvalidTokenException("Invalid or expired token.");
        }
        if (authCode == AuthErrorCode.CERTIFICATE_FETCH_FAILED
                || (authCode == null && OUTAGE.contains(platformCode))
                || e.getCause() instanceof IOException) {
            log.warn("Firebase unavailable during token verification: authErrorCode={}, errorCode={}",
                    authCode, platformCode);
            return new AuthServiceUnavailableException("Authentication service unavailable.");
        }
        log.error("Unexpected Firebase verification error: authErrorCode={}, errorCode={}", authCode, platformCode);
        return new AuthVerificationException("Token verification failed.");
    }
}
