package com.inventory.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.dto.CategoryStock;
import com.inventory.dto.DashboardResponse;
import com.inventory.dto.LowStockProduct;
import com.inventory.service.DashboardService;
import com.inventory.support.WebSliceSupport;

/** DashboardService is a mock: these tests check only the JSON shape. */
@WebMvcTest(controllers = DashboardController.class)
class DashboardControllerTest extends WebSliceSupport {

    @MockitoBean
    private DashboardService service;

    @Test
    void bigintFieldsAreStrings() throws Exception {
        when(service.get()).thenReturn(new DashboardResponse(0, BigInteger.ZERO, 0, List.of(), List.of(),
                List.of(new CategoryStock("Electronics", BigInteger.ZERO))));

        mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStockQuantity").value("0"))
                .andExpect(jsonPath("$.stockByCategory[0].totalQuantity").value("0"))
                .andExpect(jsonPath("$.totalProducts").value(0));
    }

    @Test
    void aggregateAboveLongSerialisesExactly() throws Exception {
        when(service.get()).thenReturn(new DashboardResponse(2, new BigInteger("18446744073709551614"), 0,
                List.of(), List.of(), List.of()));

        mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(content().string(Matchers.containsString("\"totalStockQuantity\":\"18446744073709551614\"")));
    }

    @Test
    void idsAreJsonNumbers() throws Exception {
        when(service.get()).thenReturn(new DashboardResponse(1, BigInteger.TWO, 1,
                List.of(new LowStockProduct(9L, "ELEC-001", "Mouse", 2, 5)), List.of(), List.of()));

        mvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(content().string(Matchers.containsString("\"id\":9,")))
                .andExpect(jsonPath("$.lowStockProducts[0].quantity").value("2"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}
