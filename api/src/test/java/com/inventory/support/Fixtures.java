package com.inventory.support;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** JDBC test data builders for data-writing integration tests (inventory_test only). Returns generated ids. */
public final class Fixtures {

    private final JdbcTemplate jdbc;

    public Fixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long category(String name) {
        return insert("INSERT INTO categories (name, created_at, updated_at)"
                + " VALUES (?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))", name);
    }

    public long supplier(String name) {
        return insert("INSERT INTO suppliers (name, created_at, updated_at)"
                + " VALUES (?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))", name);
    }

    public long product(String sku, String name, long categoryId, Long supplierId, long quantity, int reorderLevel,
            boolean active) {
        return insert("INSERT INTO products (sku, name, category_id, supplier_id, price, quantity, reorder_level,"
                + " active, created_at, updated_at) VALUES (?, ?, ?, ?, 10.00, ?, ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                sku, name, categoryId, supplierId, quantity, reorderLevel, active);
    }

    public long user(String firebaseUid, String email) {
        return insert("INSERT INTO users (firebase_uid, email, display_name, role, created_at, updated_at)"
                + " VALUES (?, ?, NULL, 'USER', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))", firebaseUid, email);
    }

    /** createdAtUtc like "2026-01-01 00:00:00.000000" (UTC). */
    public long transaction(long productId, long userId, String type, int quantity, long quantityAfter,
            String createdAtUtc) {
        return insert("INSERT INTO stock_transactions (product_id, user_id, type, quantity, quantity_after, note,"
                + " created_at) VALUES (?, ?, ?, ?, ?, NULL, ?)", productId, userId, type, quantity, quantityAfter,
                createdAtUtc);
    }

    private long insert(String sql, Object... args) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
