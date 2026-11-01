package com.inventory.support;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.inventory.config.TestDatabaseGuard;

/**
 * Removes all rows from inventory_test, child tables first so foreign keys are never violated.
 * Uses DELETE only (no TRUNCATE/DDL) and re-checks the database name before every run.
 */
public final class DatabaseCleaner {

    static final List<String> DELETE_STATEMENTS = List.of(
            "DELETE FROM stock_transactions",
            "DELETE FROM products",
            "DELETE FROM users",
            "DELETE FROM suppliers",
            "DELETE FROM categories");

    private DatabaseCleaner() {
    }

    public static void deleteAll(JdbcTemplate jdbc) {
        TestDatabaseGuard.requireTestDatabase(jdbc.queryForObject("SELECT DATABASE()", String.class));
        DELETE_STATEMENTS.forEach(jdbc::update);
    }
}
