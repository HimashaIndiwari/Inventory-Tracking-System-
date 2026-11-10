package com.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Stock adjustment: a signed delta (positive adds stock, negative removes it). The note is the required reason.
 * A zero delta is rejected in StockService because it is a rule about the value, not just its shape.
 */
public record StockAdjustmentRequest(
        @NotNull Long productId,
        @NotNull @Min(-1_000_000) @Max(1_000_000) Integer delta,
        @NotBlank @Size(max = 255) String note) {
}
