package com.inventory.support;

import org.springframework.boot.test.context.TestComponent;

import com.inventory.security.AuthServiceUnavailableException;
import com.inventory.security.AuthVerificationException;
import com.inventory.security.FirebaseTokenVerifier;
import com.inventory.security.InvalidTokenException;
import com.inventory.security.VerifiedToken;

/**
 * Test double: maps fixed token strings to verification outcomes. @TestComponent keeps it out of component
 * scanning; it is used only where explicitly @Import-ed.
 */
@TestComponent
public class FakeFirebaseTokenVerifier implements FirebaseTokenVerifier {

    @Override
    public VerifiedToken verify(String idToken) {
        return switch (idToken) {
            case "user-token" -> new VerifiedToken("u-user", "user@example.com", true, "Test User");
            case "admin-token" -> new VerifiedToken("u-admin", "admin@example.com", true, "Test Admin");
            case "unverified-admin-token" -> new VerifiedToken("u-admin2", "admin@example.com", false, null);
            case "no-email-token" -> new VerifiedToken("u-noemail", null, false, null);
            case "firebase-down-token" -> throw new AuthServiceUnavailableException("Authentication service unavailable.");
            case "boom-token" -> throw new AuthVerificationException("Token verification failed.");
            default -> throw new InvalidTokenException("Invalid or expired token."); // revoked/disabled/expired/unknown
        };
    }
}
