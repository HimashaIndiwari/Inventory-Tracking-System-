package com.inventory.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import com.inventory.controller.AuthController;
import com.inventory.support.WebSliceSupport;

/**
 * Security rules at Task 6 time: catalog and stock controllers do not exist yet, so a request that passes
 * security ends in 404 (no handler). The assertion is "neither 401 nor 403".
 */
@WebMvcTest(controllers = AuthController.class)
class SecurityRulesTest extends WebSliceSupport {

    private static final String[] CATALOG = {"/api/products", "/api/categories/1", "/api/suppliers/1"};

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void revokedTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer revoked-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void disabledTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer disabled-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void firebaseOutageIs503() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer firebase-down-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTH_SERVICE_UNAVAILABLE"));
    }

    @Test
    void unexpectedVerifierFailureIs500() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer boom-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void userCannotWriteCatalog() throws Exception {
        for (String path : CATALOG) {
            for (RequestBuilder request : writes(path, USER)) {
                mvc.perform(request)
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            }
        }
    }

    @Test
    void adminCanWriteCatalog() throws Exception {
        for (String path : CATALOG) {
            for (RequestBuilder request : writes(path, ADMIN)) {
                MvcResult result = mvc.perform(request).andReturn();
                assertThat(result.getResponse().getStatus()).as(path).isNotIn(401, 403);
            }
        }
    }

    @Test
    void userCanPostStock() throws Exception {
        MvcResult result = mvc.perform(post("/api/stock/in").header(HttpHeaders.AUTHORIZATION, USER)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void unlistedPathIsDenied() throws Exception {
        mvc.perform(get("/internal/anything").header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsPreflightAllowsConfiguredOriginOnly() throws Exception {
        mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(options("/api/auth/me").header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private static RequestBuilder[] writes(String path, String auth) {
        return new RequestBuilder[] {
                post(path).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content("{}"),
                put(path).header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete(path).header(HttpHeaders.AUTHORIZATION, auth)};
    }
}
