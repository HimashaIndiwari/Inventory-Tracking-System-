package com.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(
        @NotNull Long productId,
        @NotNull @Min(1) @Max(1_000_000) Integer quantity,
        @Size(max = 255) String note) {
}
