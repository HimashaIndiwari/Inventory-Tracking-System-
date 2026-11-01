package com.inventory.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inventory.entity.TransactionType;

/** product shows the product's current SKU and name, not the values at the time of the movement. */
public record StockTransactionResponse(Long id, TransactionType type, int quantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING) long quantityAfter, String note, ProductRef product,
        UserRef user, Instant createdAt) {

    public record ProductRef(Long id, String sku, String name) {
    }

    public record UserRef(Long id, String displayName, String email) {
    }
}
