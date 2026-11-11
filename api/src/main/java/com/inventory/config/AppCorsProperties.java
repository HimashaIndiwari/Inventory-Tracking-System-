package com.inventory.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Comma-separated frontend origins allowed by CORS (APP_CORS_ALLOWED_ORIGIN). */
@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(String allowedOrigin) {

    public List<String> allowedOrigins() {
        if (allowedOrigin == null || allowedOrigin.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigin.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }
}
