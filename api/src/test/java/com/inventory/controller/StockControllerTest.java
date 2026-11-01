package com.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import com.inventory.dto.StockMovementRequest;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.dto.StockTransactionResponse.ProductRef;
import com.inventory.dto.StockTransactionResponse.UserRef;
import com.inventory.entity.TransactionType;
import com.inventory.exception.RequestValidationException;
import com.inventory.security.AuthenticatedUser;
import com.inventory.service.StockService;
import com.inventory.support.WebSliceSupport;

/** StockService is a mock: these tests check only HTTP validation and JSON shape. */
@WebMvcTest(controllers = StockController.class)
class StockControllerTest extends WebSliceSupport {

    @MockitoBean
    private StockService service;

    private static final StockTransactionResponse RESPONSE = new StockTransactionResponse(4L, TransactionType.IN, 3,
            8, null, new ProductRef(9L, "ELEC-001", "Mouse"), new UserRef(1L, null, "user@example.com"),
            Instant.parse("2026-11-01T09:30:00Z"));

    private ResultActions stockIn(String json) throws Exception {
        when(service.stockIn(any(StockMovementRequest.class), any(AuthenticatedUser.class))).thenReturn(RESPONSE);
        return mvc.perform(post("/api/stock/in").header(HttpHeaders.AUTHORIZATION, USER)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void stockInReturns201() throws Exception {
        stockIn("{\"productId\":9,\"quantity\":3}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IN"));
    }

    @Test
    void quantityAfterIsString() throws Exception {
        stockIn("{\"productId\":9,\"quantity\":3}")
                .andExpect(jsonPath("$.quantityAfter").value("8"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void quantityZeroAndAboveMillionAre400() throws Exception {
        for (String quantity : new String[] {"0", "1000001"}) {
            stockIn("{\"productId\":9,\"quantity\":" + quantity + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
        }
    }

    @Test
    void millionIsAccepted() throws Exception {
        stockIn("{\"productId\":9,\"quantity\":1000000}").andExpect(status().isCreated());
    }

    @Test
    void noteOver255Is400() throws Exception {
        stockIn("{\"productId\":9,\"quantity\":1,\"note\":\"" + "n".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("note"));
    }

    @Test
    void stockOutIsAllowedForUser() throws Exception {
        when(service.stockOut(any(StockMovementRequest.class), any(AuthenticatedUser.class))).thenReturn(RESPONSE);

        mvc.perform(post("/api/stock/out").header(HttpHeaders.AUTHORIZATION, USER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":9,\"quantity\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void historyFromAfterToIs400() throws Exception {
        when(service.history(any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenThrow(new RequestValidationException("from", "must not be after 'to'"));

        mvc.perform(get("/api/stock/history").header(HttpHeaders.AUTHORIZATION, USER)
                .param("from", "2026-02-01T00:00:00Z").param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"));
    }

    @Test
    void historyRejectsOutOfRangePaging() throws Exception {
        for (String[] params : new String[][] {{"size", "101"}, {"size", "0"}, {"page", "-1"}}) {
            mvc.perform(get("/api/stock/history").param(params[0], params[1]).header(HttpHeaders.AUTHORIZATION, USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void invalidTypeParamIs400Malformed() throws Exception {
        mvc.perform(get("/api/stock/history").param("type", "SIDEWAYS").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
