package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import com.inventory.dto.StockMovementRequest;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.entity.Role;
import com.inventory.exception.InsufficientStockException;
import com.inventory.security.AuthenticatedUser;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G6): includes committed concurrent transactions, removed by the cleaner. */
class StockServiceIT extends DataWritingIntegrationSupport {

    @Autowired
    private StockService stockService;

    private long quantity(long productId) {
        return jdbc.queryForObject("SELECT quantity FROM products WHERE id = ?", Long.class, productId);
    }

    private long transactionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions", Long.class);
    }

    private static AuthenticatedUser actor(long userId) {
        return new AuthenticatedUser(userId, "u-it", "it@example.com", null, Role.USER);
    }

    @Test
    void concurrentStockOutOfLastUnitsAllowsExactlyOne() throws Exception {
        Fixtures f = new Fixtures(jdbc);
        long product = f.product("CONC-1", "Last units", f.category("Concurrency"), null, 5, 0, true);
        AuthenticatedUser user = actor(f.user("u-it", "it@example.com"));

        CountDownLatch start = new CountDownLatch(1);
        Callable<StockTransactionResponse> takeAll = () -> {
            start.await();
            return stockService.stockOut(new StockMovementRequest(product, 5, null), user);
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<StockTransactionResponse>> results = new ArrayList<>();
        try {
            results.add(pool.submit(takeAll));
            results.add(pool.submit(takeAll));
            start.countDown();

            int successes = 0;
            int insufficient = 0;
            for (Future<StockTransactionResponse> result : results) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(InsufficientStockException.class);
                    insufficient++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(insufficient).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(quantity(product)).isZero();
        assertThat(transactionCount()).isEqualTo(1);
    }

    @Test
    void failedTransactionInsertRollsBackQuantity() {
        Fixtures f = new Fixtures(jdbc);
        long product = f.product("RB-1", "Rollback", f.category("Rollback"), null, 5, 0, true);
        AuthenticatedUser missingUser = actor(Long.MAX_VALUE);

        assertThrows(DataIntegrityViolationException.class,
                () -> stockService.stockIn(new StockMovementRequest(product, 3, null), missingUser));

        assertThat(quantity(product)).isEqualTo(5);
        assertThat(transactionCount()).isZero();
    }

    @Test
    void databaseCheckRejectsNegativeQuantity() {
        Fixtures f = new Fixtures(jdbc);
        long product = f.product("CHK-1", "Check", f.category("Check"), null, 5, 0, true);

        DataAccessException e = assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE products SET quantity = -1 WHERE id = ?", product));

        Throwable root = NestedExceptionUtils.getRootCause(e);
        assertThat(root).isInstanceOf(SQLException.class);
        assertThat(((SQLException) root).getErrorCode()).isEqualTo(3819);
        assertThat(quantity(product)).isEqualTo(5);
    }

    @Test
    void historyShowsCurrentProductNameAfterRename() {
        Fixtures f = new Fixtures(jdbc);
        long product = f.product("REN-1", "Old name", f.category("Rename"), null, 0, 0, true);
        AuthenticatedUser user = actor(f.user("u-it", "it@example.com"));
        stockService.stockIn(new StockMovementRequest(product, 2, null), user);

        jdbc.update("UPDATE products SET name = 'New name' WHERE id = ?", product);

        assertThat(stockService.history(product, null, null, null, null, 0, 20).content())
                .extracting(t -> t.product().name()).containsExactly("New name");
    }
}
