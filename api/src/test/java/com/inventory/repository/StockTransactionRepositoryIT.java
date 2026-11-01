package com.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.inventory.entity.StockTransaction;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G1). */
class StockTransactionRepositoryIT extends DataWritingIntegrationSupport {

    private static final PageRequest NEWEST_FIRST =
            PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    @Autowired
    private StockTransactionRepository transactions;

    @Test
    void historyIncludesArchivedProducts() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Cleaning");
        long archived = f.product("CLN-X", "Archived spray", cat, null, 0, 0, false);
        long user = f.user("uid-1", "a@example.com");
        long tx = f.transaction(archived, user, "OUT", 3, 0, "2026-06-16 12:00:00.000000");

        assertThat(ids(transactions.history(null, null, null, null, null, NEWEST_FIRST).getContent()))
                .containsExactly(tx);
    }

    @Test
    void historyFromInclusiveToExclusive() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Electronics");
        long product = f.product("ELEC-X", "Mouse", cat, null, 20, 0, true);
        long user = f.user("uid-1", "a@example.com");
        long atFrom = f.transaction(product, user, "IN", 20, 20, "2026-01-01 00:00:00.000000");
        long between = f.transaction(product, user, "OUT", 1, 19, "2026-06-01 00:00:00.000000");
        f.transaction(product, user, "OUT", 1, 18, "2027-01-01 00:00:00.000000");

        List<StockTransaction> result = transactions.history(null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"), NEWEST_FIRST)
                .getContent();

        assertThat(ids(result)).containsExactly(between, atFrom);
    }

    @Test
    void historyOrdersByCreatedAtDescThenIdDesc() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Office");
        long product = f.product("OFF-X", "Paper", cat, null, 2, 0, true);
        long user = f.user("uid-1", "a@example.com");
        long first = f.transaction(product, user, "IN", 1, 1, "2026-03-01 10:00:00.000000");
        long second = f.transaction(product, user, "IN", 1, 2, "2026-03-01 10:00:00.000000");

        assertThat(ids(transactions.history(null, null, null, null, null, NEWEST_FIRST).getContent()))
                .containsExactly(second, first);
    }

    private static List<Long> ids(List<StockTransaction> list) {
        return list.stream().map(StockTransaction::getId).toList();
    }
}
