package com.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.repository.CategoryRepository;
import com.inventory.support.DataWritingIntegrationSupport;

/** Data-writing (gate G1). DATETIME(6) values written as UTC are read back as the same Instant. */
class TimestampRoundTripIT extends DataWritingIntegrationSupport {

    @Autowired
    private CategoryRepository categories;

    @Test
    void instantRoundTripsAsUtc() {
        jdbc.update("INSERT INTO categories (name, created_at, updated_at)"
                + " VALUES ('Clock', '2026-11-01 09:30:00.123456', '2026-11-01 09:30:00.123456')");

        Instant createdAt = categories.findAll().get(0).getCreatedAt();

        assertThat(createdAt).isEqualTo(Instant.parse("2026-11-01T09:30:00.123456Z"));
    }
}
