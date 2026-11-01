package com.inventory.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Base for integration tests that only read from inventory_test. No cleanup, so no approval gate. */
@SpringBootTest
@Import(FakeFirebaseTokenVerifier.class)
@ActiveProfiles("test")
public abstract class ReadOnlyIntegrationSupport {

    @Autowired
    protected JdbcTemplate jdbc;
}
