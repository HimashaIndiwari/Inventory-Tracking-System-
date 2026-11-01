package com.inventory.dto;

import java.time.Instant;

/** productCount counts active and archived products (archived products still block deletion). */
public record SupplierResponse(Long id, String name, String contactPerson, String email, String phone,
        String address, long productCount, Instant createdAt, Instant updatedAt) {
}
