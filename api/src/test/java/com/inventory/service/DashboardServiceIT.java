package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.dto.CategoryStock;
import com.inventory.dto.DashboardResponse;
import com.inventory.dto.LowStockProduct;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G7). */
class DashboardServiceIT extends DataWritingIntegrationSupport {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void emptyInventoryReturnsZeros() {
        DashboardResponse d = dashboardService.get();

        assertThat(d.totalProducts()).isZero();
        assertThat(d.totalStockQuantity()).isEqualTo(BigInteger.ZERO);
        assertThat(d.lowStockCount()).isZero();
        assertThat(d.lowStockProducts()).isEmpty();
        assertThat(d.recentTransactions()).isEmpty();
        assertThat(d.stockByCategory()).isEmpty();
    }

    @Test
    void categoriesWithoutProductsReportZero() {
        Fixtures f = new Fixtures(jdbc);
        f.category("A");
        f.category("B");

        assertThat(dashboardService.get().stockByCategory()).containsExactly(
                new CategoryStock("A", BigInteger.ZERO), new CategoryStock("B", BigInteger.ZERO));
    }

    @Test
    void totalStockAboveLongIsExact() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Huge");
        f.product("HUGE-1", "One", cat, null, Long.MAX_VALUE, 0, true);
        f.product("HUGE-2", "Two", cat, null, Long.MAX_VALUE, 0, true);

        assertThat(dashboardService.get().totalStockQuantity()).isEqualTo(new BigInteger("18446744073709551614"));
    }

    @Test
    void figuresIgnoreArchivedProducts() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Mixed");
        f.product("ACT-1", "Active", cat, null, 3, 5, true);
        f.product("ARC-1", "Archived", cat, null, 0, 5, false);

        DashboardResponse d = dashboardService.get();

        assertThat(d.totalProducts()).isEqualTo(1);
        assertThat(d.totalStockQuantity()).isEqualTo(BigInteger.valueOf(3));
        assertThat(d.lowStockCount()).isEqualTo(1);
        assertThat(d.lowStockProducts()).extracting(LowStockProduct::sku).containsExactly("ACT-1");
    }

    @Test
    void lowStockListIsTop5OrderedByQuantity() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Low");
        for (int i = 6; i >= 1; i--) {
            f.product("LOW-" + i, "Low " + i, cat, null, i, 10, true);
        }

        assertThat(dashboardService.get().lowStockProducts()).extracting(LowStockProduct::quantity)
                .containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void recentTransactionsAreLatest10() {
        Fixtures f = new Fixtures(jdbc);
        long product = f.product("TX-1", "Tx", f.category("Tx"), null, 0, 0, true);
        long user = f.user("u-dash", "dash@example.com");
        for (int i = 1; i <= 12; i++) {
            f.transaction(product, user, "IN", 1, i, String.format("2026-01-%02d 00:00:00.000000", i));
        }

        assertThat(dashboardService.get().recentTransactions()).extracting(StockTransactionResponse::quantityAfter)
                .containsExactly(12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L);
    }
}
