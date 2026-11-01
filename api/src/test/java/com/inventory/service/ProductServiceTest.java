package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.dto.ProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.ProductHasStockException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;

class ProductServiceTest {

    private ProductRepository products;
    private CategoryRepository categories;
    private ProductService service;
    private Category electronics;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        categories = mock(CategoryRepository.class);
        service = new ProductService(products, categories, mock(SupplierRepository.class));
        electronics = new Category("Electronics", null);
        ReflectionTestUtils.setField(electronics, "id", 1L);
        when(categories.findById(1L)).thenReturn(Optional.of(electronics));
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(products.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProductRequest request(String sku) {
        return new ProductRequest(sku, "Wireless Mouse", null, 1L, null, new BigDecimal("24.99"), 10);
    }

    private Product stored(long quantity, boolean active) {
        Product p = new Product("ELEC-001", "Mouse", null, electronics, null, BigDecimal.TEN, 5);
        ReflectionTestUtils.setField(p, "id", 9L);
        p.changeQuantity(quantity);
        if (!active) {
            p.archive();
        }
        return p;
    }

    @Test
    void createNormalisesSkuAndStartsAtZero() {
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);

        ProductResponse response = service.create(request(" elec-009 "));

        verify(products).save(saved.capture());
        assertThat(saved.getValue().getSku()).isEqualTo("ELEC-009");
        assertThat(saved.getValue().getQuantity()).isZero();
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(response.sku()).isEqualTo("ELEC-009");
    }

    @Test
    void createRejectsNormalisedDuplicateSku() {
        when(products.existsBySku("ELEC-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(request(" elec-001 ")));
        verify(products, never()).save(any());
    }

    @Test
    void normaliseSkuTrimsAndUppercases() {
        assertThat(ProductService.normaliseSku("  elec-001\t")).isEqualTo("ELEC-001");
    }

    @Test
    void normalisedSkuKeepsExactLengthLimits() {
        assertThat(ProductService.normaliseSku(" abc ")).hasSize(3);
        assertThat(ProductService.normaliseSku(" " + "a".repeat(50) + " ")).hasSize(50);
    }

    @Test
    void unknownCategoryIs404() {
        when(categories.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(request("ELEC-001")));
    }

    @Test
    void updateIgnoresQuantity() {
        Product p = stored(7, true);
        when(products.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(p));

        service.update(9L, request("ELEC-001"));

        assertThat(p.getQuantity()).isEqualTo(7);
    }

    @Test
    void getArchivedIs404() {
        when(products.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(9L));
    }

    @Test
    void updateArchivedIs404() {
        when(products.findByIdAndActiveTrue(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9L, request("ELEC-001")));
    }

    @Test
    void archiveWithStockIsBlocked() {
        when(products.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(stored(3, true)));

        assertThrows(ProductHasStockException.class, () -> service.archive(9L));
    }

    @Test
    void archiveWithZeroStockSetsInactive() {
        Product p = stored(0, true);
        when(products.findByIdAndActiveTrue(9L)).thenReturn(Optional.of(p));

        service.archive(9L);

        assertThat(p.isActive()).isFalse();
    }
}
