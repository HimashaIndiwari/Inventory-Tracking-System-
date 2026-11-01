package com.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Whether the Firebase Admin SDK is initialised; disabled in the test profile. */
@ConfigurationProperties(prefix = "app.firebase")
public record AppFirebaseProperties(@DefaultValue("true") boolean enabled) {
}
