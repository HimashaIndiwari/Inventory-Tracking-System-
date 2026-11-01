package com.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.inventory.exception.GlobalExceptionHandlerTest;
import com.inventory.support.ReadOnlyIntegrationSupport;

/** Read-only: starts the full context against inventory_test (ddl-auto=validate) without changing data. */
class ApplicationContextIT extends ReadOnlyIntegrationSupport {

    @Autowired
    private ApplicationContext context;

    @Test
    void usesTestDatabase() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("inventory_test");
    }

    @Test
    void testOnlyBeansAreNotComponentScanned() {
        assertThat(context.getBeanNamesForType(GlobalExceptionHandlerTest.ThrowingController.class)).isEmpty();
    }
}
