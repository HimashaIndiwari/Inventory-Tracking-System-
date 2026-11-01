package com.inventory.security;

/** Claims taken from a verified Firebase ID token. The token itself is never stored or logged. */
public record VerifiedToken(String uid, String email, boolean emailVerified, String name) {
}
