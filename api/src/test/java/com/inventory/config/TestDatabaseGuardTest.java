package com.inventory.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TestDatabaseGuardTest {

    @Test
    void acceptsTestSuffix() {
        assertDoesNotThrow(() -> TestDatabaseGuard.requireTestDatabase("inventory_test"));
    }

    @Test
    void rejectsDevelopmentDb() {
        assertThrows(IllegalStateException.class, () -> TestDatabaseGuard.requireTestDatabase("inventory_db"));
    }

    @Test
    void rejectsNullAndLookalike() {
        assertThrows(IllegalStateException.class, () -> TestDatabaseGuard.requireTestDatabase(null));
        assertThrows(IllegalStateException.class, () -> TestDatabaseGuard.requireTestDatabase("inventory_test_backup"));
    }
}
