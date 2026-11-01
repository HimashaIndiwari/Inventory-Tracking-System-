package com.inventory.repository;

import java.math.BigDecimal;

/** Projection of the native stock-by-category query; totals are exact DECIMAL(65,0) values. */
public interface CategoryStockRow {

    String getCategoryName();

    BigDecimal getTotalQuantity();
}
