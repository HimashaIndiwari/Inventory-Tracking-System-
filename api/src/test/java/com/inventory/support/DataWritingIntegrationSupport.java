package com.inventory.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for integration tests that write to inventory_test. Deletes all rows before each test and after the
 * class. Test methods must not be @Transactional: data is committed for real and removed only here.
 * PER_CLASS lets the @AfterAll cleanup be an instance method that uses the injected JdbcTemplate.
 */
@SpringBootTest
@Import(FakeFirebaseTokenVerifier.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class DataWritingIntegrationSupport {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanBeforeEachTest() {
        DatabaseCleaner.deleteAll(jdbc);
    }

    @AfterAll
    void cleanAfterTestClass() {
        DatabaseCleaner.deleteAll(jdbc);
    }
}
