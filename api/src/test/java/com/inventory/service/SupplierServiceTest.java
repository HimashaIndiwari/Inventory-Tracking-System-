package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.dto.SupplierRequest;
import com.inventory.dto.SupplierResponse;
import com.inventory.entity.Supplier;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.ResourceInUseException;
import com.inventory.repository.NameCount;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;

class SupplierServiceTest {

    private SupplierRepository suppliers;
    private ProductRepository products;
    private SupplierService service;

    @BeforeEach
    void setUp() {
        suppliers = mock(SupplierRepository.class);
        products = mock(ProductRepository.class);
        when(suppliers.save(any(Supplier.class))).thenAnswer(inv -> withId(inv.getArgument(0), 5L));
        when(suppliers.saveAndFlush(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new SupplierService(suppliers, products);
    }

    private static Supplier withId(Supplier supplier, long id) {
        ReflectionTestUtils.setField(supplier, "id", id);
        return supplier;
    }

    @Test
    void createTrimsFieldsAndBlanksBecomeNull() {
        SupplierResponse created = service.create(
                new SupplierRequest(" Northwind ", " ", " sales@example.com ", "", null));

        assertThat(created.name()).isEqualTo("Northwind");
        assertThat(created.contactPerson()).isNull();
        assertThat(created.email()).isEqualTo("sales@example.com");
        assertThat(created.phone()).isNull();
    }

    @Test
    void createRejectsDuplicateName() {
        when(suppliers.existsByName("NORTHWIND")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(new SupplierRequest("NORTHWIND", null, null, null, null)));
    }

    @Test
    void deleteBlockedWhenReferenced() {
        when(suppliers.findById(5L)).thenReturn(Optional.of(withId(new Supplier("S", null, null, null, null), 5L)));
        when(products.existsBySupplierId(5L)).thenReturn(true);

        assertThrows(ResourceInUseException.class, () -> service.delete(5L));
        verify(suppliers, never()).delete(any());
    }

    @Test
    void listIncludesProductCount() {
        when(suppliers.findAllByOrderByNameAsc())
                .thenReturn(List.of(withId(new Supplier("A", null, null, null, null), 1L)));
        when(products.countBySupplier()).thenReturn(List.of(new NameCount(1L, 2)));

        assertThat(service.list()).extracting(SupplierResponse::productCount).containsExactly(2L);
    }
}
