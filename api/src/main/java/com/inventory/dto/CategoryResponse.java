package com.inventory.dto;

import java.time.Instant;

/** productCount counts active and archived products (archived products still block deletion). */
public record CategoryResponse(Long id, String name, String description, long productCount, Instant createdAt,
        Instant updatedAt) {
}
