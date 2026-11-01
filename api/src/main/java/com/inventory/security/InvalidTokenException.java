package com.inventory.security;

/** Invalid, expired or revoked token, or a disabled/deleted user: 401. */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
