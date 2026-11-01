package com.inventory.security;

/** Firebase could not be reached (certificates, network, service): 503, users stay signed in. */
public class AuthServiceUnavailableException extends RuntimeException {

    public AuthServiceUnavailableException(String message) {
        super(message);
    }
}
