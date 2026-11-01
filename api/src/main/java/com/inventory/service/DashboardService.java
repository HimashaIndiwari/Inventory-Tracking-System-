package com.inventory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.DashboardResponse;
import com.inventory.dto.LowStockProduct;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;

/** Dashboard figures (spec §3.2) cover active products only; recent transactions include archived products. */
@Service
public class DashboardService {

    private final ProductRepository products;
    private final StockTransactionRepository transactions;

    public DashboardService(ProductRepository products, StockTransactionRepository transactions) {
        this.products = products;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        return new DashboardResponse(
                products.countByActiveTrue(),
                products.sumActiveQuantity(),
                products.countActiveLowStock(),
                products.findTop5ActiveLowStock().stream()
                        .map(p -> new LowStockProduct(p.getId(), p.getSku(), p.getName(), p.getQuantity(),
                                p.getReorderLevel()))
                        .toList(),
                transactions.findTop10ByOrderByCreatedAtDescIdDesc().stream()
                        .map(StockService::toResponse)
                        .toList(),
                products.stockByCategory());
    }
}
