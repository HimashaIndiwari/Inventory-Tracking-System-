package com.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.inventory.dto.CategoryStock;
import com.inventory.entity.Product;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G1). */
class ProductRepositoryIT extends DataWritingIntegrationSupport {

    private static final long LONG_MAX = Long.MAX_VALUE;

    @Autowired
    private ProductRepository products;

    private Fixtures fixtures() {
        return new Fixtures(jdbc);
    }

    private static PageRequest byNameThenId() {
        return PageRequest.of(0, 20, Sort.by("name", "id"));
    }

    @Test
    void searchExcludesArchivedAndOrdersByNameThenId() {
        Fixtures f = fixtures();
        long cat = f.category("Electronics");
        long first = f.product("MOUSE-1", "Mouse", cat, null, 1, 0, true);
        long second = f.product("MOUSE-2", "Mouse", cat, null, 1, 0, true);
        f.product("MOUSE-3", "Mouse", cat, null, 1, 0, false);

        List<Long> ids = products.search("%mouse%", null, false, byNameThenId()).map(Product::getId).getContent();

        assertThat(ids).containsExactly(first, second);
    }

    /** Task 8 addition (gate G5): "%" and "_" in search input match literally. */
    @Test
    void searchTreatsWildcardsLiterally() {
        Fixtures f = fixtures();
        long cat = f.category("Labels");
        long offer = f.product("LBL-1", "50% Off Labels", cat, null, 1, 0, true);
        f.product("LBL-2", "Plain Labels", cat, null, 1, 0, true);

        List<Long> ids = products.search(com.inventory.util.LikePatterns.contains("50%"), null, false, byNameThenId())
                .map(Product::getId).getContent();

        assertThat(ids).containsExactly(offer);
    }

    @Test
    void lowStockFilterUsesLessOrEqual() {
        Fixtures f = fixtures();
        long cat = f.category("Office");
        long atLevel = f.product("LOW-1", "At level", cat, null, 5, 5, true);
        f.product("OK-1", "Above level", cat, null, 6, 5, true);

        List<Long> ids = products.search(null, null, true, byNameThenId()).map(Product::getId).getContent();

        assertThat(ids).containsExactly(atLevel);
    }

    @Test
    void countByCategoryIncludesArchived() {
        Fixtures f = fixtures();
        long cat = f.category("Furniture");
        f.product("FUR-A", "Active", cat, null, 0, 0, true);
        f.product("FUR-B", "Archived", cat, null, 0, 0, false);

        assertThat(products.countByCategory()).containsExactly(new NameCount(cat, 2));
        assertThat(products.existsByCategoryId(cat)).isTrue();
    }

    @Test
    void dashboardAggregatesIgnoreArchived() {
        Fixtures f = fixtures();
        long cat = f.category("Networking");
        f.product("NET-A", "Active low", cat, null, 2, 5, true);
        f.product("NET-B", "Active ok", cat, null, 10, 5, true);
        f.product("NET-C", "Archived", cat, null, 0, 5, false);

        assertThat(products.countByActiveTrue()).isEqualTo(2);
        assertThat(products.sumActiveQuantity()).isEqualTo(BigInteger.valueOf(12));
        assertThat(products.countActiveLowStock()).isEqualTo(1);
    }

    @Test
    void sumActiveQuantityIsZeroForEmptyInventory() {
        assertThat(products.sumActiveQuantity()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void sumActiveQuantityIsZeroWhenAllArchived() {
        Fixtures f = fixtures();
        long cat = f.category("Old");
        f.product("OLD-1", "Archived", cat, null, 0, 0, false);

        assertThat(products.sumActiveQuantity()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    void stockByCategoryIsZeroForCategoryWithoutActiveProducts() {
        Fixtures f = fixtures();
        f.category("Empty");

        assertThat(products.stockByCategory()).containsExactly(new CategoryStock("Empty", BigInteger.ZERO));
    }

    @Test
    void sumActiveQuantityExceedsLongExactly() {
        Fixtures f = fixtures();
        long cat = f.category("Huge");
        f.product("HUGE-1", "Huge one", cat, null, LONG_MAX, 0, true);
        f.product("HUGE-2", "Huge two", cat, null, LONG_MAX, 0, true);

        assertThat(products.sumActiveQuantity()).isEqualTo(new BigInteger("18446744073709551614"));
    }

    @Test
    void stockByCategoryExceedsLongExactly() {
        Fixtures f = fixtures();
        long cat = f.category("Huge");
        f.product("HUGE-1", "Huge one", cat, null, LONG_MAX, 0, true);
        f.product("HUGE-2", "Huge two", cat, null, LONG_MAX, 0, true);

        assertThat(products.stockByCategory())
                .containsExactly(new CategoryStock("Huge", new BigInteger("18446744073709551614")));
    }
}
