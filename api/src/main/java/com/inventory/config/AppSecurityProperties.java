package com.inventory.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Admin allowlist from APP_SECURITY_ADMIN_EMAILS (comma-separated). Never exposed to the frontend. */
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(List<String> adminEmails) {

    public AppSecurityProperties {
        adminEmails = adminEmails == null ? List.of() : List.copyOf(adminEmails);
    }
}
