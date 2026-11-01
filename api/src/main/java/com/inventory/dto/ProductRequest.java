package com.inventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** No quantity: stock changes only through stock movements. The SKU is normalised by ProductService. */
public record ProductRequest(
        @NotBlank @Pattern(regexp = "^\\s*[A-Za-z0-9-]{3,50}\\s*$",
                message = "must be 3-50 characters: letters, digits and hyphens") String sku,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull Long categoryId,
        Long supplierId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer reorderLevel) {
}
