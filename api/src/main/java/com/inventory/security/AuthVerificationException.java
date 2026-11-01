package com.inventory.security;

/** Any other verification failure (e.g. misconfigured credentials): 500 with a generic message. */
public class AuthVerificationException extends RuntimeException {

    public AuthVerificationException(String message) {
        super(message);
    }
}
