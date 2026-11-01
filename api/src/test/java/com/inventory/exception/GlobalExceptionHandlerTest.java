package com.inventory.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.security.ProblemDetailWriter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.json.JsonMapper;

public class GlobalExceptionHandlerTest {

    record Body(@NotBlank String name) {
    }

    /** Nested in a test class, so Spring Boot's test filter keeps it out of component scanning. */
    @RestController
    public static class ThrowingController {

        @PostMapping("/t/validated")
        String validated(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/t/param")
        String param(@RequestParam @Min(1) int size) {
            return "ok";
        }

        @GetMapping("/t/typed")
        String typed(@RequestParam Long id) {
            return "ok";
        }

        @GetMapping("/t/insufficient")
        String insufficient() {
            throw new InsufficientStockException(8, 5);
        }

        @GetMapping("/t/request-validation")
        String requestValidation() {
            throw new RequestValidationException("from", "must not be after 'to'");
        }

        @GetMapping("/t/duplicate")
        String duplicate() {
            throw ConstraintViolationClassifierTest.chain("23000", 1062, "uk_products_sku");
        }

        @GetMapping("/t/in-use")
        String inUse() {
            throw ConstraintViolationClassifierTest.chain("23000", 1217, "fk_products_category");
        }

        @GetMapping("/t/parent-missing")
        String parentMissing() {
            throw ConstraintViolationClassifierTest.chain("23000", 1216, null);
        }

        @GetMapping("/t/boom")
        String boom() {
            throw new IllegalStateException("SELECT secret FROM users");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ProblemDetailWriter writer = new ProblemDetailWriter(JsonMapper.builder().build());
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(writer))
                .build();
    }

    @Test
    void validationErrorHasFieldErrors() throws Exception {
        mvc.perform(post("/t/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void methodParameterValidationIs400() throws Exception {
        mvc.perform(get("/t/param").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void malformedJsonIs400Malformed() throws Exception {
        mvc.perform(post("/t/validated").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void wrongParameterTypeIs400Malformed() throws Exception {
        mvc.perform(get("/t/typed").param("id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void apiExceptionUsesItsCode() throws Exception {
        mvc.perform(get("/t/insufficient"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.detail").value("Insufficient stock: requested 8, available 5."));
    }

    @Test
    void requestValidationExceptionHasFieldError() throws Exception {
        mvc.perform(get("/t/request-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("from"));
    }

    @Test
    void duplicateMapsTo409() throws Exception {
        mvc.perform(get("/t/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(content().string(not(containsString("uk_"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("23000"))));
    }

    @Test
    void stillReferencedMapsTo409() throws Exception {
        mvc.perform(get("/t/in-use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_IN_USE"));
    }

    @Test
    void parentMissingMapsTo404() throws Exception {
        mvc.perform(get("/t/parent-missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unexpectedErrorHidesInternals() throws Exception {
        mvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at com."))));
    }

    @Test
    void everyErrorHasStandardFields() throws Exception {
        mvc.perform(get("/t/insufficient"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.instance").value("/t/insufficient"))
                .andExpect(jsonPath("$.fieldErrors").value(empty()));
    }
}
