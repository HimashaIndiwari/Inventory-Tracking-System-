package com.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.dto.SupplierRequest;
import com.inventory.dto.SupplierResponse;
import com.inventory.service.SupplierService;
import com.inventory.support.WebSliceSupport;

@WebMvcTest(controllers = SupplierController.class)
class SupplierControllerTest extends WebSliceSupport {

    @MockitoBean
    private SupplierService service;

    @Test
    void postReturns201WithLocation() throws Exception {
        Instant now = Instant.parse("2026-11-01T09:30:00Z");
        when(service.create(any(SupplierRequest.class)))
                .thenReturn(new SupplierResponse(4L, "Northwind", null, null, null, null, 0, now, now));

        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Northwind\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/suppliers/4"));
    }

    @Test
    void invalidSupplierEmailIs400() throws Exception {
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"N\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void phoneOver30CharsIs400() throws Exception {
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"phone\":\"" + "1".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"));
    }

    @Test
    void userCannotCreateSupplier() throws Exception {
        mvc.perform(post("/api/suppliers").header(HttpHeaders.AUTHORIZATION, USER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"N\"}"))
                .andExpect(status().isForbidden());
    }
}
