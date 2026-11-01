package com.inventory.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.dto.MeResponse;
import com.inventory.security.AuthenticatedUser;

/** Returns the user already resolved by FirebaseAuthFilter; performs no provisioning itself. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new MeResponse(user.userId(), user.email(), user.displayName(), user.role().name());
    }
}
