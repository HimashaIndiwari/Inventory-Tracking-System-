package com.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.dto.CategoryRequest;
import com.inventory.dto.CategoryResponse;
import com.inventory.exception.ResourceInUseException;
import com.inventory.service.CategoryService;
import com.inventory.support.WebSliceSupport;

@WebMvcTest(controllers = CategoryController.class)
class CategoryControllerTest extends WebSliceSupport {

    @MockitoBean
    private CategoryService service;

    private static final Instant NOW = Instant.parse("2026-11-01T09:30:00Z");

    @Test
    void listIsVisibleToUser() throws Exception {
        when(service.list()).thenReturn(List.of(new CategoryResponse(1L, "Electronics", null, 3, NOW, NOW)));

        mvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].productCount").value(3))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-01T09:30:00Z"));
    }

    @Test
    void postReturns201WithLocation() throws Exception {
        when(service.create(any(CategoryRequest.class))).thenReturn(new CategoryResponse(7L, "Tools", null, 0, NOW, NOW));

        mvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tools\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/categories/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void blankNameIs400WithFieldError() throws Exception {
        mvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void nameOver100CharsIs400() throws Exception {
        mvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putReturns200() throws Exception {
        when(service.update(eq(7L), any(CategoryRequest.class)))
                .thenReturn(new CategoryResponse(7L, "Tools", "Hand tools", 0, NOW, NOW));

        mvc.perform(put("/api/categories/7").header(HttpHeaders.AUTHORIZATION, ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tools\",\"description\":\"Hand tools\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Hand tools"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/categories/7").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteInUseIs409ResourceInUse() throws Exception {
        doThrow(new ResourceInUseException("This category is used by one or more products."))
                .when(service).delete(7L);

        mvc.perform(delete("/api/categories/7").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }
}
