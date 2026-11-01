package com.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The single frontend origin allowed by CORS (APP_CORS_ALLOWED_ORIGIN). */
@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(String allowedOrigin) {
}
