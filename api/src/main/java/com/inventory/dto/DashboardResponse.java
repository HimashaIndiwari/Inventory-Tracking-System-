package com.inventory.dto;

import java.math.BigInteger;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/** totalStockQuantity can exceed a signed 64-bit value, so it stays a BigInteger and is sent as a JSON string. */
public record DashboardResponse(long totalProducts,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigInteger totalStockQuantity, long lowStockCount,
        List<LowStockProduct> lowStockProducts, List<StockTransactionResponse> recentTransactions,
        List<CategoryStock> stockByCategory) {
}
