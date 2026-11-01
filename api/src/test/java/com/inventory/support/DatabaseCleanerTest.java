package com.inventory.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseCleanerTest {

    @Test
    void refusesNonTestDatabaseBeforeDeleting() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT DATABASE()", String.class)).thenReturn("inventory_db");

        assertThrows(IllegalStateException.class, () -> DatabaseCleaner.deleteAll(jdbc));

        verify(jdbc, never()).update(startsWith("DELETE"));
    }

    @Test
    void deletesChildTablesBeforeParents() {
        assertThat(DatabaseCleaner.DELETE_STATEMENTS).containsExactly(
                "DELETE FROM stock_transactions",
                "DELETE FROM products",
                "DELETE FROM users",
                "DELETE FROM suppliers",
                "DELETE FROM categories");
    }

    @Test
    void deletesInOrderOnTestDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT DATABASE()", String.class)).thenReturn("inventory_test");

        DatabaseCleaner.deleteAll(jdbc);

        InOrder order = inOrder(jdbc);
        for (String statement : DatabaseCleaner.DELETE_STATEMENTS) {
            order.verify(jdbc).update(statement);
        }
    }
}
