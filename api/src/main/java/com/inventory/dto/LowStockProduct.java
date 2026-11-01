package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

public record LowStockProduct(Long id, String sku, String name,
        @JsonFormat(shape = JsonFormat.Shape.STRING) long quantity, int reorderLevel) {
}
