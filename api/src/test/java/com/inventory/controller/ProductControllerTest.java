package com.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.dto.IdName;
import com.inventory.dto.PageResponse;
import com.inventory.dto.ProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.exception.ProductHasStockException;
import com.inventory.service.ProductService;
import com.inventory.support.WebSliceSupport;

/** ProductService is a mock: these tests check only HTTP validation and JSON shape. */
@WebMvcTest(controllers = ProductController.class)
class ProductControllerTest extends WebSliceSupport {

    @MockitoBean
    private ProductService service;

    private static final Instant NOW = Instant.parse("2026-11-01T09:30:00Z");

    private static ProductResponse response(String sku, long quantity) {
        return new ProductResponse(9L, sku, "Mouse", null, new IdName(1L, "Electronics"), null,
                new BigDecimal("24.99"), quantity, 10, quantity <= 10, NOW, NOW);
    }

    private static String body(String sku) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"Mouse\",\"categoryId\":1,\"price\":24.99,\"reorderLevel\":10}";
    }

    private org.springframework.test.web.servlet.ResultActions create(String json) throws Exception {
        when(service.create(any(ProductRequest.class))).thenReturn(response("ELEC-009", 0));
        return mvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, ADMIN)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void postReturns201WithLocation() throws Exception {
        create(body("ELEC-009"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/products/9"));
    }

    @Test
    void quantityIsSerialisedAsString() throws Exception {
        create(body("ELEC-009")).andExpect(jsonPath("$.quantity").value("0"))
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void sentQuantityIsIgnoredOnCreate() throws Exception {
        create("{\"sku\":\"ELEC-009\",\"name\":\"Mouse\",\"categoryId\":1,\"price\":1,\"reorderLevel\":1,\"quantity\":999}")
                .andExpect(status().isCreated());
    }

    @Test
    void skuWithTwoCharCoreIs400() throws Exception {
        create(body("AB")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
    }

    @Test
    void skuWithFiftyOneCharCoreIs400() throws Exception {
        create(body("A".repeat(51))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
    }

    @Test
    void skuWithFiftyCharCoreIsAccepted() throws Exception {
        create(body("A".repeat(50))).andExpect(status().isCreated());
        verify(service).create(argThat(r -> r.sku().equals("A".repeat(50))));
    }

    @Test
    void skuWithInnerSpaceIs400() throws Exception {
        create(body("EL EC-1")).andExpect(status().isBadRequest());
    }

    @Test
    void skuWithNonAsciiLetterIs400() throws Exception {
        create(body("ÉLEC-1")).andExpect(status().isBadRequest());
    }

    @Test
    void skuPaddedWithNonBreakingSpaceIs400() throws Exception {
        create(body(" ELEC-1")).andExpect(status().isBadRequest());
    }

    @Test
    void paddedLowercaseSkuPassesValidation() throws Exception {
        create(body(" elec-009 ")).andExpect(status().isCreated());
    }

    @Test
    void negativePriceIs400() throws Exception {
        create("{\"sku\":\"ELEC-9\",\"name\":\"M\",\"categoryId\":1,\"price\":-0.01,\"reorderLevel\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    void threeDecimalPriceIs400() throws Exception {
        create("{\"sku\":\"ELEC-9\",\"name\":\"M\",\"categoryId\":1,\"price\":1.005,\"reorderLevel\":0}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsPage() throws Exception {
        when(service.search(eq("mouse"), eq(null), eq(false), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(response("ELEC-001", 15)), 0, 20, 1, 1));

        mvc.perform(get("/api/products").param("search", "mouse").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("ELEC-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsOutOfRangePaging() throws Exception {
        for (String[] params : new String[][] {{"size", "101"}, {"size", "0"}, {"page", "-1"}}) {
            mvc.perform(get("/api/products").param(params[0], params[1]).header(HttpHeaders.AUTHORIZATION, USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void archiveWithStockIs409ProductHasStock() throws Exception {
        doThrow(new ProductHasStockException(3)).when(service).archive(9L);

        mvc.perform(delete("/api/products/9").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_HAS_STOCK"));
    }

    @Test
    void archiveReturns204() throws Exception {
        mvc.perform(delete("/api/products/9").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNoContent());
    }
}
