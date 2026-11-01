package com.inventory.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.inventory.exception.ErrorCode;
import com.inventory.service.UserProvisioningService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates "Authorization: Bearer <Firebase ID token>" (spec §4.3): verify the token, resolve the MySQL
 * user and role, and put an AuthenticatedUser into the SecurityContext. Not a Spring bean (created in
 * SecurityConfig) so it is not registered twice as a servlet filter.
 */
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final FirebaseTokenVerifier verifier;
    private final UserProvisioningService provisioning;
    private final ProblemDetailWriter writer;

    public FirebaseAuthFilter(FirebaseTokenVerifier verifier, UserProvisioningService provisioning,
            ProblemDetailWriter writer) {
        this.verifier = verifier;
        this.provisioning = provisioning;
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            chain.doFilter(request, response); // unauthenticated; protected paths answer 401 via the entry point
            return;
        }
        String idToken = header.substring(BEARER.length()).trim();
        AuthenticatedUser user;
        try {
            if (idToken.isEmpty()) {
                throw new InvalidTokenException("Empty bearer token.");
            }
            user = provisioning.resolve(verifier.verify(idToken));
        } catch (InvalidTokenException e) {
            writer.write(response, ErrorCode.UNAUTHENTICATED, "Invalid or expired authentication token.",
                    request.getRequestURI());
            return;
        } catch (AuthServiceUnavailableException e) {
            writer.write(response, ErrorCode.AUTH_SERVICE_UNAVAILABLE,
                    "The authentication service is temporarily unavailable. Please try again.",
                    request.getRequestURI());
            return;
        } catch (RuntimeException e) {
            log.error("Authentication failed unexpectedly on {}", request.getRequestURI(), e);
            writer.write(response, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.", request.getRequestURI());
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.authorities()));
        chain.doFilter(request, response);
    }
}
