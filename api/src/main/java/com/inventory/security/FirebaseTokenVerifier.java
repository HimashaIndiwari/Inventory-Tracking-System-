package com.inventory.security;

/**
 * Verifies a Firebase ID token. Implementations throw only {@link InvalidTokenException} (401),
 * {@link AuthServiceUnavailableException} (503) or {@link AuthVerificationException} (500).
 */
public interface FirebaseTokenVerifier {

    VerifiedToken verify(String idToken);
}
