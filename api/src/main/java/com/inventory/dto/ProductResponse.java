package com.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ProductResponse(Long id, String sku, String name, String description, IdName category,
        IdName supplier, BigDecimal price, @JsonFormat(shape = JsonFormat.Shape.STRING) long quantity,
        int reorderLevel, boolean lowStock, Instant createdAt, Instant updatedAt) {
}
