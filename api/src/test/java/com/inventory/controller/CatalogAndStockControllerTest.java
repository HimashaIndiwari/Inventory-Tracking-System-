package com.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.dto.ProductResponse;
import com.inventory.dto.SupplierResponse;
import com.inventory.service.ProductService;
import com.inventory.service.StockService;
import com.inventory.service.SupplierService;
import com.inventory.support.WebSliceSupport;

/**
 * Endpoint behaviour not already asserted in ProductControllerTest, SupplierControllerTest and StockControllerTest:
 * product update, supplier update and delete, and required-field validation for product, supplier and stock
 * requests (the other tests check only the first field error or a single field). Services are mocked, so no
 * database is used. Security rules are tested in SecurityRulesTest.
 */
@WebMvcTest(controllers = {ProductController.class, SupplierController.class, StockController.class})
class CatalogAndStockControllerTest extends WebSliceSupport {

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private StockService stockService;

    private static final Instant NOW = Instant.parse("2026-11-01T09:30:00Z");

    private static final String PRODUCT_JSON =
            "{\"sku\":\"ELEC-001\",\"name\":\"Mouse\",\"categoryId\":1,\"price\":24.99,\"reorderLevel\":5}";

    private static ProductResponse product(long id) {
        return new ProductResponse(id, "ELEC-001", "Mouse", null, null, null, new BigDecimal("24.99"), 0L, 5,
                false, NOW, NOW);
    }

    private static SupplierResponse supplier(long id) {
        return new SupplierResponse(id, "Northwind Traders", null, null, null, null, 0L, NOW, NOW);
    }

    @Test
    void adminUpdatesProductWith200() throws Exception {
        when(productService.update(eq(9L), any())).thenReturn(product(9L));

        mvc.perform(put("/api/products/9").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
        verify(productService).update(eq(9L), any());
    }

    @Test
    void productCreateWithEmptyBodyIs400ListingEveryRequiredField() throws Exception {
        mvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(5));
        verify(productService, never()).create(any());
    }

    @Test
    void adminUpdatesSupplierWith200() throws Exception {
        when(supplierService.update(eq(4L), any())).thenReturn(supplier(4L));

        mvc.perform(put("/api/suppliers/4").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Northwind Traders\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
        verify(supplierService).update(eq(4L), any());
    }

    @Test
    void adminDeletesSupplierWith204() throws Exception {
        mvc.perform(delete("/api/suppliers/4").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNoContent());
        verify(supplierService).delete(4L);
    }

    @Test
    void supplierCreateWithEmptyBodyIs400ListingName() throws Exception {
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        verify(supplierService, never()).create(any());
    }

    @Test
    void stockInWithEmptyBodyIs400ListingProductAndQuantity() throws Exception {
        mvc.perform(post("/api/stock/in").header(HttpHeaders.AUTHORIZATION, USER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
        verify(stockService, never()).stockIn(any(), any());
    }

    @Test
    void stockAdjustWithEmptyBodyIs400ListingProductDeltaAndNote() throws Exception {
        mvc.perform(post("/api/stock/adjust").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
        verify(stockService, never()).adjust(any(), any());
    }
}
