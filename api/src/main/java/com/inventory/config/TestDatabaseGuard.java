package com.inventory.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the test and e2e profiles against any database whose name does not end with _test,
 * so automated tests can never touch inventory_db or a production database.
 */
@Component
@Profile({"test", "e2e"})
public class TestDatabaseGuard {

    public TestDatabaseGuard(DataSource dataSource) {
        requireTestDatabase(new JdbcTemplate(dataSource).queryForObject("SELECT DATABASE()", String.class));
    }

    public static void requireTestDatabase(String name) {
        if (name == null || !name.endsWith("_test")) {
            throw new IllegalStateException(
                    "Refusing to use database '" + name + "': name must end with _test");
        }
    }
}
